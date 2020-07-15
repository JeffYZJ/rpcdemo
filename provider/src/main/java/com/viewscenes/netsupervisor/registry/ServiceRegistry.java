package com.viewscenes.netsupervisor.registry;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 服务注册类，将Server服务注册到zookeeper中去
 */
@Component
public class ServiceRegistry {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Value("${registry.address}")
    private String registryAddress;
    private static final String ZK_REGISTRY_PATH = "/rpc";

    public void register(String data) {
        if (data != null) {
            ZkClient client = connectServer();
            if (client != null) {
                AddRootNode(client);
                createNode(client, data);
            }
        }
    }
    //连接zookeeper
    private ZkClient connectServer() {
        ZkClient client = new ZkClient(registryAddress,20000,20000);
        return client;
    }

    /**
     * 子节点必须是临时节点
     * 生产者端停掉之后，才能通知到消费者，把此服务从服务列表中剔除
     * @param client
     */
    //创建根目录/rpc
    private void AddRootNode(ZkClient client){
        boolean exists = client.exists(ZK_REGISTRY_PATH);
        if (!exists){
            client.createPersistent(ZK_REGISTRY_PATH);
            logger.info("创建zookeeper主节点 {}",ZK_REGISTRY_PATH);
        }
    }
    //在/rpc根目录下，创建临时顺序子节点
    private void createNode(ZkClient client, String data) {
        String path = client.create(ZK_REGISTRY_PATH + "/provider", data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        logger.info("创建zookeeper数据节点 ({} => {})", path, data);
    }

}
