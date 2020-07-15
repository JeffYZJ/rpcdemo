import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.Map;

public class ZkClientUtil {
    ZkClient zkClient;
    //   private String connectString="192.168.0.31:2181,192.168.0.32:2181,192.168.0.33:2181";
    private String connectString = "192.168.0.105:2181";

    //   public ZkClientUtil() {
//      this.zkClient = new ZkClient(connectString,5000,5000,new SerializableSerializer());  // zk默认的序列化方式
//   }

    public ZkClientUtil() {
        //定义序列化方式，别忘了
        this.zkClient = new ZkClient(connectString, 5000, 5000, new MyZkSerializer());
    }

    public void createPersistent(String path) {
        zkClient.createPersistent(path, true); //创建持久化节点，true表示如果父节点不存在则创建父节点
    }

    //创建 永久 节点，并设置数据
    public void createPersistent(String path, Object data) {
        zkClient.createPersistent(path, data);
    }

    // 创建永久 有序节点
    public void createPersistentSequential(String path, Object data) {
        zkClient.createEphemeralSequential(path, data);
    }

    //创建临时节点  会话失效后删除
    public void createEphemeral(String path, Object data) {
        zkClient.createEphemeral(path, data);
    }

    //创建 临时节点 有序 节点   会话失效后删除
    public void createEphemeralSequential(String path, Object data) {
        zkClient.createEphemeralSequential(path, data);
    }

    //创建alc节点
    public void createAcl(String path, Object data, final List<ACL> acl, final CreateMode mode) {
        zkClient.create(path, data, acl, mode);
    }

    //设置acl 属性
    public void setAcl(String path, List<ACL> acl) {
        zkClient.setAcl(path, acl);
    }

    //获得acl属性
    public Map.Entry<List<ACL>, Stat> getAcl(String path) {
        return zkClient.getAcl(path);
    }


    //读取数据
    public Object readData(String path) {
//      return zkClient.readData(path);
        //没有不会抛异常，而是返回null
        return zkClient.readData(path, true);
    }

    /**
     * 读取 子节点 只能找 其 子一级 下 所有的
     */
    public List<String> getChildren(String path) {
        return zkClient.getChildren(path);
    }

    /**
     * 递归查找 所有 子节点
     */
    public void getChilderRecursive(String path) {
        System.out.println(path);
        if (zkClient.exists(path)) {
            List<String> list = zkClient.getChildren(path);
            if (list.size() > 0) {
                list.stream().forEach(n -> {
                    getChilderRecursive(path + "/" + n);
                });
            }
        }

    }

    // 更新内容
    public void writeData(String path, Object object) {
        zkClient.writeData(path, object);
    }

    //删除单个节点 即这个节点下不能有子节点
    public void delete(String path) {
        zkClient.delete(path);
    }


    //递归删除节点 即删除其节点下 所有子节点  对应rmr 命令
    public void deleteRecursive(String path) {
        zkClient.deleteRecursive(path);
    }


    /***
     * 支持创建递归方式 但是不能写入数据
     * @param path
     * @param createParents     true，表明需要递归创建父节点
     */
    public void createPersistentRecursive(String path, boolean createParents) {
        zkClient.createPersistent(path, createParents);
    }

    /**
     * 关闭zk
     */
    public void close() {
        zkClient.close();
    }
    /**
     * 监听
     */
    public void lister(String path) {
        //对节点添加监听变化。  当前节点内容修改、节点删除 能监听数据
        zkClient.subscribeDataChanges(path, new IZkDataListener() {
            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {
                System.out.printf("   变更的节点为:%s,%s", dataPath, data);       // 节点变更  变更的节点为:/w,123
            }

            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                System.out.printf("    删除的节点为:%s", dataPath);
            }
        });
        //对父节点添加监听子节点变化。监听 下面子节点的新增、删除 和 当前节点   不监听数据发生修改和变化。  parentPath: /w,currentChilds:[ww1]
        zkClient.subscribeChildChanges(path, new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                System.out.println("   parentPath: " + parentPath + ",currentChilds:" + currentChilds);
            }
        });
        //zeng  gai  shan
        //对父节点添加监听子节点变化。
        zkClient.subscribeStateChanges(new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                if (state == Watcher.Event.KeeperState.SyncConnected) {
                    //当我重新启动后start，监听触发
                    System.out.println("连接成功");
                } else if (state == Watcher.Event.KeeperState.Disconnected) {
                    System.out.println("连接断开");//当我在服务端将zk服务stop时，监听触发
                } else
                    System.out.println("其他状态" + state);
            }

            @Override
            public void handleNewSession() throws Exception {
                System.out.println("重建session");
            }

            @Override
            public void handleSessionEstablishmentError(Throwable error) throws Exception {
            }
        });

    }
}
