import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ZkClientWatcher {
    private ZkClientUtil zkClientUtil = new ZkClientUtil();
    ZkClient zkClient;

    /**
     * 新增 节点(持节+临时) 并读取数据   (不能直接创建 子节点)
     */
    @Test
    public void createPersistent() {
        User user = new User();
        user.setAge(18);
        user.setName("zjq");
        zkClientUtil.createPersistent("/ry", user);       // 创建永久节点
        System.out.println(zkClientUtil.readData("/ry"));  // 读取 节点 数据
        User user2 = new User();
        user2.setAge(23);
        user2.setName("lj");
        zkClientUtil.createEphemeral("/rh", user2);        // 创建临时节点
        System.out.println(zkClientUtil.readData("/rh"));
    }

    /**
     * 新增 节点(永久有序，临时有序)   并读取数据   (不能直接创建 子节点)
     */
    @Test
    public void create() {
        User user = new User();
        user.setAge(1);
        user.setName("我擦");
        zkClientUtil.createPersistentSequential("/ry", user);    // 创建 永久有序节点

        User user2 = new User("我擦2", 2);
        zkClientUtil.createPersistentSequential("/ry", user2);    // 创建 临时有序节点

    }

    /**
     * 创建acl节点
     */
    @Test
    public void createAcl() {
        User user = new User("acldata", 3);
        User user2 = new User("acldata2", 4);
        List<ACL> aclList = new ArrayList<>();
        int perm = ZooDefs.Perms.ADMIN | ZooDefs.Perms.READ | ZooDefs.Perms.WRITE;          // 或 运算 admin 可以设置节点访问控制列表权限
//        aclList.add(new ACL(perm, new Id("world", "anyone")));                //设置有所人的权限
//        aclList.add(new ACL(ZooDefs.Perms.ALL, new Id("ip", "127.0.0.1")));               //设置所有权限，本机ip
        zkClientUtil.createPersistent("/acl", user);                                   // 创建节点
        zkClientUtil.setAcl("/acl", aclList);                                        // 给节点设置acl权限
        //创建并设置acl节点 ZooDefs.Ids.OPEN_ACL_UNSAFE 表anyone
        zkClientUtil.createAcl("/alc2", aclList, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    /**
     * 获得 acl 属性
     */
    @Test
    public void getAndSetAcl() {
        Map.Entry acl = zkClientUtil.getAcl("/acl");
        System.out.println(acl.getKey());
        System.out.println(acl.getValue());
    }

    /**
     * 递归创建节点(但是不能写入数据) 写入数据，读数据,,再 删除子节点
     */
    @Test
    public void test() {
        String path = "/zjq/t1";
        zkClientUtil.createPersistentRecursive(path, true);
        zkClientUtil.writeData(path, new User("xx", 3));
        Object o = zkClientUtil.readData(path);
        System.out.println(o);
        zkClientUtil.deleteRecursive(path);
    }

    /**
     * 创建子节点，并读取
     */
    @Test
    public void getChildren() {
        String path = "/zjq/t1";
        String path2 = "/zjq/t2";
        String path3 = "/zjq/t3";
        zkClientUtil.createPersistentRecursive(path, true);
        zkClientUtil.createPersistentRecursive(path2, true);
        zkClientUtil.createPersistentRecursive(path3, true);
        List list = zkClientUtil.getChildren("/zjq");
        list.stream().forEach(n -> {
            System.out.println(n);
        });
    }

    /**
     * 递归查找 所有 子节点
     */
    @Test
    public void getChilderRecursive() {
        String path = "/zjq";
        zkClientUtil.getChilderRecursive(path);
    }


    /**
     *  测试监听     并且开启 下面的main方法
     */
    @Test
    public void testListen() throws InterruptedException {
        ZkClientUtil zkClientUtil = new ZkClientUtil();
        String path = "/wukong/w1";
        zkClientUtil.deleteRecursive(path);                     //先删除
        zkClientUtil.lister(path);                              //添加监听
        zkClientUtil.createPersistent(path, "123");      //再创建节点
        Thread.sleep(2000);
        zkClientUtil.writeData(path, "abc");            //修改数据
        Thread.sleep(Integer.MAX_VALUE);
    }

    public static void main(String[] args) throws InterruptedException {
        ZkClientUtil zkClientUtil=new ZkClientUtil();
        String path="/wukong/w1";
        zkClientUtil.writeData(path,"abc"); //能触发 或者在sh zkCli.sh  delete /wukong 也行
    }

}
