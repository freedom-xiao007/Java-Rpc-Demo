# RPC Demo（二） 基于 Zookeeper 的服务发现
***
## 简介
&ensp;&ensp;&ensp;&ensp;基于上篇的：[RPC Demo(一) Netty RPC Demo 实现](https://github.com/lw1243925457/JAVA-000/blob/main/homework/rpc/rpc-demo/README.md)

&ensp;&ensp;&ensp;&ensp;第二部分来实现使用Zookeeper作为服务注册中心，去掉在RPC调用中的显示传参

&ensp;&ensp;&ensp;&ensp;完整项目工程地址：[RpcDemoJava](https://github.com/lw1243925457/Java-Rpc-Demo)

## 改进说明
&ensp;&ensp;&ensp;&ensp;在客户端调用中，我们需要显示的传入后端服务器的地址，这样显的有些不方便，代码大致如下：

```java
UserService userService = jdk.create(UserService.class, "http://localhost:8080/");
```

&ensp;&ensp;&ensp;&ensp;利用Zookeeper作为注册中心，客户端可以从Zookeeper中获取接口实现的服务器相关地址，就不必再显式传入地址了，改进后大致如下：

```java
UserService userService = jdk.create(UserService.class);
```

## 编码思路
&ensp;&ensp;&ensp;&ensp;进过调研和思考，实现的思路和步骤大致如下：

- 1.服务端将Provider注册到Zookeeper中
- 2.客户端拉取所有的Provider信息到本地，建立接口（Consumer）和Provider列表的映射关系
- 3.客户端能监听服务端Provider的增删改查，同步到客户端，便于删除和更新变化后的Provider信息
- 4.客户端反射调用时从Provider列表中获取相关url地址，进行访问，返回结果

&ensp;&ensp;&ensp;&ensp;需要在本地启动一个zk，使用docker即可，相关命令如下：

```shell script
# 拉取ZK镜像启动ZK，后面的三个命令是基于运行了这个命令后的
docker run -dit --name zk -p 2181:2181 zookeeper
# 查看ZK运行日志
docker logs -f zk
# 重启ZK
docker restart zk
# 启动ZK
docker start zk
# 停止ZK
docker stop zk
```

### Provider信息结构约定
&ensp;&ensp;&ensp;&ensp;我们约定一个Provider信息如下：

```java
@Data
public class ProviderInfo {

    /**
     * Provider ID：ZK注册后会生成一个ID
     * Client 获取Provider列表时，将此ID设置为获取的ZK生成的ID
     */
    String id;

    /**
     * Provider对应的后端服务器地址
     */
    String url;

    /**
     * 标签：用于简单路由
     */
    List<String> tags;

    /**
     * 权重：用于加权负载均衡
     */
    Integer weight;

    public ProviderInfo() {}

    public ProviderInfo(String id, String url, List<String> tags, int weight) {
        this.id = id;
        this.url = url;
        this.tags = tags;
        this.weight = weight;
    }
}
```

### 1.服务端将Provider注册到Zookeeper中
&ensp;&ensp;&ensp;&ensp;首先，我们要为各个接口的实现指定Provider名称、分组、版本、标签、权重，这里我们使用注解进行实现

```java
/**
 * RPC provider service 初始化注解
 *
 * group,version,targs 都有默认值，是为了兼容以前的版本
 *
 * @author lw1243925457
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProviderService {

    /**
     * 对应 API 接口名称
     * @return API service
     */
    String service();

    /**
     * 分组
     * @return group
     */
    String group() default "default";

    /**
     * version
     * @return version
     */
    String version() default "default";

    /**
     * tags:用于简单路由
     * 多个标签使用逗号分隔
     * @return tags
     */
    String tags() default "";

    /**
     * 权重：用于加权负载均衡
     * @return
     */
    int weight() default 1;
}
```

&ensp;&ensp;&ensp;&ensp;接下来，借鉴Mybatis的设置包扫描路径的思路，写一个通过扫描指定包路径下的所有的class，获取class后判断其是否是Provider（有相应的注解），如果是，提取信息，注册到ZK
中，大致的代码如下：

```java
/**
 * 提供RPC Provider 的初始化
 * 初始化实例放入 Map 中，方便后续的获取
 *
 * @author lw1243925457
 */
@Slf4j
public class ProviderServiceManagement {

    /**
     * 通过服务名、分组、版本作为key，确实接口实现类的实例
     * service:group:version --> class
     */
    private static Map<String, Object> proxyMap = new HashMap<>();

    /**
     * 初始化：通过扫描包路径，获取所有实现类，将其注册到ZK中
     * 获取实现类上的Provider注解，获取服务名、分组、版本
     * 调用ZK服务注册，将Provider注册到ZK中
     * @param packageName 接口实现类的包路径
     * @param port 服务监听的端口
     * @throws Exception exception
     */
    public static void init(String packageName, int port) throws Exception {
        System.out.println("\n-------- Loader Rpc Provider class start ----------------------\n");

        DiscoveryServer serviceRegister = new DiscoveryServer();

        Class[] classes = getClasses(packageName);
        for (Class c: classes) {
            ProviderService annotation = (ProviderService) c.getAnnotation(ProviderService.class);
            if (annotation == null) {
                continue;
            }
            String group = annotation.group();
            String version = annotation.version();
            List<String> tags = Arrays.asList(annotation.tags().split(","));
            String provider = Joiner.on(":").join(annotation.service(), group, version);
            int weight = annotation.weight();

            proxyMap.put(provider, c.newInstance());

            serviceRegister.registerService(annotation.service(), group, version, port, tags, weight);

            log.info("load provider class: " + annotation.service() + ":" + group + ":" + version + " :: " + c.getName());
        }
        System.out.println("\n-------- Loader Rpc Provider class end ----------------------\n");
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException exception
     * @throws IOException exception
     */
    private static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[0]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }
}
```

&ensp;&ensp;&ensp;&ensp;接下来该写ZK服务注册的相关代码，这块查查资料就能写出来了，大致如下：

```java
/**
 * ZK客户端，用于连接ZK
 * 
 * @author lw1243925457
 */
@Slf4j
public class ZookeeperClient {

    static final String REGISTER_ROOT_PATH = "rpc";

    protected CuratorFramework client;

    ZookeeperClient() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        this.client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .namespace(REGISTER_ROOT_PATH)
                .retryPolicy(retryPolicy)
                .build();
        this.client.start();

        log.info("zookeeper service register init");
    }
}


/**
 * 服务发现服务器：用于注册Provider
 *
 * @author lw1243925457
 */
public class DiscoveryServer extends ZookeeperClient {

    private List<ServiceDiscovery<ProviderInfo>> discoveryList = new ArrayList<>();

    public DiscoveryServer() {
    }

    /**
     * 生成Provider的相关信息，注册到ZK中
     * @param service Service impl name
     * @param group group
     * @param version version
     * @param port service listen port
     * @param tags route tags
     * @param weight load balance weight
     * @throws Exception exception
     */
    public void registerService(String service, String group, String version, int port, List<String> tags,
                                int weight) throws Exception {
        ProviderInfo provider = new ProviderInfo(null, null, tags, weight);

        ServiceInstance<ProviderInfo> instance = ServiceInstance.<ProviderInfo>builder()
                .name(Joiner.on(":").join(service, group, version))
                .port(port)
                .address(InetAddress.getLocalHost().getHostAddress())
                .payload(provider)
                .build();

        JsonInstanceSerializer<ProviderInfo> serializer = new JsonInstanceSerializer<>(ProviderInfo.class);
        ServiceDiscovery<ProviderInfo> discovery = ServiceDiscoveryBuilder.builder(ProviderInfo.class)
                .client(client)
                .basePath(REGISTER_ROOT_PATH)
                .thisInstance(instance)
                .serializer(serializer)
                .build();
        discovery.start();

        discoveryList.add(discovery);
    }

    public void close() throws IOException {
        for (ServiceDiscovery<ProviderInfo> discovery: discoveryList) {
            discovery.close();
        }
        client.close();
    }
}
```

&ensp;&ensp;&ensp;&ensp;到这，服务端的核心代码基本写完了，给接口实现类加上相应的注解，启动服务器即可：

```java
/**
 * @author lw
 */
@ProviderService(service = "com.rpc.demo.service.UserService", group = "group2", version = "v2", tags = "tag2")
public class UserServiceV2Impl implements UserService {

    @Override
    public User findById(Integer id) {
        return new User(id, "RPC group2 v2");
    }
}


public class ServerApplication {

    public static void main(String[] args) throws Exception {
        BackListFilter.addBackAddress("172.21.16.1");

        final int port = 8080;
        ProviderServiceManagement.init("com.rpc.server.demo.service.impl", port);

        final RpcNettyServer rpcNettyServer = new RpcNettyServer(port);

        try {
            rpcNettyServer.run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rpcNettyServer.destroy();
        }
    }
}
```

### 2.客户端相应代码编写
- 2.客户端拉取所有的Provider信息到本地，建立接口（Consumer）和Provider列表的映射关系
- 3.客户端能监听服务端Provider的增删改查，同步到客户端，便于删除和更新变化后的Provider信息
- 4.客户端反射调用时从Provider列表中获取相关url地址，进行访问，返回结果

&ensp;&ensp;&ensp;&ensp;上面都是客户端需要增加的功能，我们直接写一个服务发现客户端，在其中实现相关的功能，大致代码如下：

```java
/**
 * 服务发现客户端
 * 获取Provider列表
 * 监听Provider更新
 * 查找返回接口的Provider（先tag路由，后负载均衡）
 * 
 * @author lw1243925457
 */
@Slf4j
public class DiscoveryClient extends ZookeeperClient {

    private enum EnumSingleton {
        /**
         * 懒汉枚举单例
         */
        INSTANCE;
        private DiscoveryClient instance;

        EnumSingleton(){
            instance = new DiscoveryClient();
        }
        public DiscoveryClient getSingleton(){
            return instance;
        }
    }

    public static DiscoveryClient getInstance(){
        return EnumSingleton.INSTANCE.getSingleton();
    }

    /**
     * Provider缓存列表
     * server:group:version -> provider instance list
     */
    private Map<String, List<ProviderInfo>> providersCache = new HashMap<>();

    private final ServiceDiscovery<ProviderInfo> serviceDiscovery;

    private final CuratorCache resourcesCache;

    private LoadBalance balance = new WeightBalance();

    private DiscoveryClient() {
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ProviderInfo.class)
                .client(client)
                .basePath("/" + REGISTER_ROOT_PATH)
                .build();

        try {
            serviceDiscovery.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            getAllProviders();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.resourcesCache = CuratorCache.build(this.client, "/");
        watchResources();

        if (RpcClient.getBalanceAlgorithmName().equals(WeightBalance.NAME)) {
            this.balance = new WeightBalance();
        }
        else if (RpcClient.getBalanceAlgorithmName().equals(ConsistentHashBalance.NAME)) {
            this.balance = new ConsistentHashBalance();
        }
    }

    /**
     * 从ZK中获取所有的Provider列表，保存下来
     * @throws Exception exception
     */
    private void getAllProviders() throws Exception {
        System.out.println("\n\n======================= init : get all provider");

        Collection<String>  serviceNames = serviceDiscovery.queryForNames();
        System.out.println(serviceNames.size() + " type(s)");
        for ( String serviceName : serviceNames ) {
            Collection<ServiceInstance<ProviderInfo>> instances = serviceDiscovery.queryForInstances(serviceName);
            System.out.println(serviceName);

            for ( ServiceInstance<ProviderInfo> instance : instances ) {
                System.out.println(instance.toString());

                String url = "http://" + instance.getAddress() + ":" + instance.getPort();
                ProviderInfo providerInfo = instance.getPayload();
                providerInfo.setId(instance.getId());
                providerInfo.setUrl(url);

                List<ProviderInfo> providerList = providersCache.getOrDefault(instance.getName(), new ArrayList<>());
                providerList.add(providerInfo);
                providersCache.put(instance.getName(), providerList);

                System.out.println("add provider: " + instance.toString());
            }
        }

        System.out.println();
        for(String key: providersCache.keySet()) {
            System.out.println(key + " : " + providersCache.get(key));
        }

        System.out.println("======================= init : get all provider end\n\n");
    }

    /**
     * 根据传入的接口名称、分组、版本，返回讲过tag路由，负载均衡后的一个Provider服务器地址
     * @param service service name
     * @param group group
     * @param version version
     * @param tags tags
     * @param methodName method name
     * @return provider host ip
     */
    public String getProviders(String service, String group, String version, List<String> tags, String methodName) {
        String provider = Joiner.on(":").join(service, group, version);
        if (!providersCache.containsKey(provider) || providersCache.get(provider).isEmpty()) {
            return null;
        }

        List<ProviderInfo> providers = FilterLine.filter(providersCache.get(provider), tags);
        if (providers.isEmpty()) {
            return null;
        }

        return balance.select(providers, service, methodName);
    }

    /**
     * 监听Provider的更新
     */
    private void watchResources() {
        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forCreates(this::addHandler)
                .forChanges(this::changeHandler)
                .forDeletes(this::deleteHandler)
                .forInitialized(() -> log.info("Resources Cache initialized"))
                .build();
        resourcesCache.listenable().addListener(listener);
        resourcesCache.start();
    }

    /**
     * 增加Provider
     * @param node new provider
     */
    private void addHandler(ChildData node) {
        System.out.println("\n\n=================== add new provider ============================");

        System.out.printf("Node created: [%s:%s]%n", node.getPath(), new String(node.getData()));
        if (providerDataEmpty(node)) {
            return;
        }

        updateProvider(node);
        
        System.out.println("=================== add new provider end ============================\n\n");
    }

    /**
     * Provider更新
     * @param oldNode old provider
     * @param newNode updated provider
     */
    private void changeHandler(ChildData oldNode, ChildData newNode) {
        System.out.printf("Node changed, Old: [%s: %s] New: [%s: %s]%n", oldNode.getPath(),
                new String(oldNode.getData()), newNode.getPath(), new String(newNode.getData()));

        if (providerDataEmpty(newNode)) {
            return;
        } 
        
        updateProvider(newNode);
    }

    /**
     * 增加或更新本地Provider
     * @param newNode updated provider
     */
    private void updateProvider(ChildData newNode) {
        String jsonValue = new String(newNode.getData(), StandardCharsets.UTF_8);
        JSONObject instance = (JSONObject) JSONObject.parse(jsonValue);
        System.out.println(instance.toString());

        String url = "http://" + instance.get("address") + ":" + instance.get("port");
        ProviderInfo providerInfo = JSON.parseObject(instance.get("payload").toString(), ProviderInfo.class);
        providerInfo.setId(instance.get("id").toString());
        providerInfo.setUrl(url);

        List<ProviderInfo> providerList = providersCache.getOrDefault(instance.get("name").toString(), new ArrayList<>());
        providerList.add(providerInfo);
        providersCache.put(instance.get("name").toString(), providerList);
    }

    /**
     * 删除Provider
     * @param oldNode provider
     */
    private void deleteHandler(ChildData oldNode) {
        System.out.println("\n\n=================== delete provider ============================");

        System.out.printf("Node deleted, Old value: [%s: %s]%n", oldNode.getPath(), new String(oldNode.getData()));
        if (providerDataEmpty(oldNode)) {
            return;
        }

        String jsonValue = new String(oldNode.getData(), StandardCharsets.UTF_8);
        JSONObject instance = (JSONObject) JSONObject.parse(jsonValue);
        System.out.println(instance.toString());

        String provider = instance.get("name").toString();
        int deleteIndex = -1;
        for (int i = 0; i < providersCache.get(provider).size(); i++) {
            if (providersCache.get(provider).get(i).getId().equals(instance.get("id").toString())) {
                deleteIndex = i;
                break;
            }
        }

        if (deleteIndex != -1) {
            providersCache.get(provider).remove(deleteIndex);
        }

        System.out.println("=================== delete provider end ============================\n\n");
    }

    private boolean providerDataEmpty(ChildData node) {
        return node.getData().length == 0;
    }

    public synchronized void close() {
        client.close();
    }
}
```

&ensp;&ensp;&ensp;&ensp;看着有点多，但不是太复杂，理清思路自己也能写出来

&ensp;&ensp;&ensp;&ensp;接下来是代理请求的修改，在：RpcInvocationHandler，中去掉显式的url传参，改为url从DiscoveryClient中获取，大致如下：

```java
public class RpcInvocationHandler implements InvocationHandler, MethodInterceptor {

    /**
     * 发送请求到服务端
     * 获取结果后序列号成对象，返回
     * @param service service name
     * @param method service method
     * @param params method params
     * @return object
     */
    private Object process(Class<?> service, Method method, Object[] params) {
        log.info("Client proxy instance method invoke");

        // 自定义了Rpc请求的结构 RpcRequest,放入接口名称、方法名、参数
        log.info("Build Rpc request");
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceClass(service.getName());
        rpcRequest.setMethod(method.getName());
        rpcRequest.setArgv(params);
        rpcRequest.setGroup(group);
        rpcRequest.setVersion(version);

        // 从DiscoveryClient中获取某个Provider的请求地址
        String url = null;
        try {
            url = discoveryClient.getProviders(service.getName(), group, version, tags, method.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (url == null) {
            System.out.println("\nCan't find provider\n");
            return null;
        }

        // 客户端使用的 netty，发送请求到服务端，拿到结果（自定义结构：rpcfxResponse)
        log.info("Client send request to Server");
        RpcResponse rpcResponse;
        try {
            rpcResponse = RpcNettyClientSync.getInstance().getResponse(rpcRequest, url);
        } catch (InterruptedException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

        log.info("Client receive response Object");
        assert rpcResponse != null;
        if (!rpcResponse.getStatus()) {
            log.info("Client receive exception");
            rpcResponse.getException().printStackTrace();
            return null;
        }

        // 序列化成对象返回
        log.info("Response:: " + rpcResponse.getResult());
        return JSON.parse(rpcResponse.getResult().toString());
    }
}
```

&ensp;&ensp;&ensp;&ensp;客户端代码也是去掉url，更加简洁，大致如下：

```java
public class ClientApplication {

    public static void main(String[] args) {
        // fastjson auto setting
        ParserConfig.getGlobalInstance().addAccept("com.rpc.demo.model.Order");
        ParserConfig.getGlobalInstance().addAccept("com.rpc.demo.model.User");

        RpcClient client = new RpcClient();
        RpcClient.setBalanceAlgorithmName(ConsistentHashBalance.NAME);

        UserService userService = client.create(UserService.class, "group2", "v2");
        User user = userService.findById(1);
        if (user == null) {
            log.info("Clint service invoke Error");
        } else {
            System.out.println("\n\nuser1 :: find user id=1 from server: " + user.getName());
        }
    }
}
```