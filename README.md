# Java RPC Dome
***
## 简介
使用Java实现大致的 RPC 框架

## 运行说明
- 服务端：rpc-server工程的ServerApplication（后缀为2的也可以启动，达到provider list的目的）
- 客户端：rpc-client工程的ClientApplication

## 实现过程中的文档记录
- [RPC Demo(一) Netty RPC Demo 实现](https://github.com/lw1243925457/JAVA-000/blob/main/homework/rpc/rpc-demo/README.md)
- [RPC Demo（二） 基于 Zookeeper 的服务发现](https://github.com/lw1243925457/Java-Rpc-Demo/blob/main/doc/zkDiscovery.md)

## 功能要求

- [x] 3、（必做）改造自定义RPC的程序，提交到github：
  - [x] 1）尝试将服务端写死查找接口实现类变成泛型和反射
  - [x] 2）尝试将客户端动态代理改成AOP，添加异常处理
  - [x] 3）尝试使用Netty+HTTP作为client端传输方式
  
- [ ] 1）尝试使用压测并分析优化RPC性能
- [x] 2）尝试使用Netty+TCP作为两端传输方式
- [ ] 3）尝试自定义二进制序列化
- [ ] 4）尝试压测改进后的RPC并分析优化，有问题欢迎群里讨论
- [ ] 5）尝试将fastjson改成xstream
- [x] 6）尝试使用字节码生成方式代替服务端反射

- [x] 1、（选做）rpcfx1.1: 给自定义RPC实现简单的分组(group)和版本(version)。
- [x] 2、（选做）rpcfx2.0: 给自定义RPC实现：
  - [x] 1）基于zookeeper的注册中心，消费者和生产者可以根据注册中心查找可用服务进行调用(直接选择列表里的最后一个)。
  - [x] 2）当有生产者启动或者下线时，通过zookeeper通知并更新各个消费者，使得各个消费者可以调用新生产者或者不调用下线生产者。
- [ ] 3、（挑战☆）在2.0的基础上继续增强rpcfx实现：
  - [ ] 1）3.0: 实现基于zookeeper的配置中心，消费者和生产者可以根据配置中心配置参数（分组，版本，线程池大小等）。
  - [ ] 2）3.1：实现基于zookeeper的元数据中心，将服务描述元数据保存到元数据中心。
  - [ ] 3）3.2：实现基于etcd/nacos/apollo等基座的配置/注册/元数据中心。

- [ ] 4、（挑战☆☆）在3.2的基础上继续增强rpcfx实现：
  - [x] 1）4.0：实现基于tag的简单路由；
  - [x] 2）4.1：实现基于Weight/ConsistentHash的负载均衡;
  - [x] 3）4.2：实现基于IP黑名单的简单流控；
  - [ ] 4）4.3：完善RPC框架里的超时处理，增加重试参数；
- [ ] 5、（挑战☆☆☆）在4.3的基础上继续增强rpcfx实现：
  - [ ] 1）5.0：实现利用HTTP头跨进程传递Context参数（隐式传参）；
  - [ ] 2）5.1：实现消费端mock一个指定对象的功能（Mock功能）；
  - [ ] 3）5.2：实现消费端可以通过一个泛化接口调用不同服务（泛化调用）；
  - [ ] 4）5.3：实现基于Weight/ConsistentHash的负载均衡;
  - [ ] 5）5.4：实现基于单位时间调用次数的流控，可以基于令牌桶等算法；
- [ ] 6、（挑战☆☆☆☆）6.0：压测，并分析调优5.4版本

## 实现过程中的思考与实现记录

- [RPC Demo 1.0 实现](https://github.com/lw1243925457/JAVA-000/blob/main/homework/rpc/rpc-demo/README.md)

## 参考链接
- [Curator 事件监听](https://www.cnblogs.com/crazymakercircle/p/10228385.html)
- [一文聊透 Dubbo 元数据中心](https://cloud.tencent.com/developer/article/1532948)
- [Netty中ctx.writeAndFlush与ctx.channel().writeAndFlush的区别](https://blog.csdn.net/FishSeeker/article/details/78447684)