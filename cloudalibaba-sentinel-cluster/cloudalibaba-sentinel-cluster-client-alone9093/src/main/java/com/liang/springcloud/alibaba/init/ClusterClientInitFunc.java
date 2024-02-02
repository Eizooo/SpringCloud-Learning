package com.liang.springcloud.alibaba.init;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.zookeeper.ZookeeperDataSource;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.liang.springcloud.alibaba.Constants;

import java.util.List;

/**
 * @PROJECT_NAME: SpringCloud-Learning
 * @USER: yuliang
 * @DESCRIPTION:
 * @DATE: 2021-04-01 17:47
 */
public class ClusterClientInitFunc implements InitFunc {

    //项目名称
    private static final String APP_NAME = AppNameUtil.getAppName();

    //nacos集群地址
    private final String remoteAddress = "127.0.0.1:2181";

    //nacos配置的分组名称
    //zookeeper中的父目录(上级目录)
    //可以将集群中的持久化配置按照不同应用进行分组
    //不同分组内, 不同的应用可以有不同的默认flowRule配置, 可以通过APP_NAME来进行隔离 == spring.application.name?
    //可视化sentinel管理界面需要同步进行改造? 目前在sentinel-dashboard中进行新建的rule与应用期望获取的目录不一致
    /*
    * 集群模式下,
    *   一个分组对应一个GroupId
    *       1.有多个APP, app_name + -flow-rules
    *       2.同一个token-server  sentinel_cluster_server_config
    *       3.同一个token-client超时配置, sentinel_cluster_client_config
    * */
    private final String groupId = "sentinel_pay_group";

    //项目名称 + Constants的配置名称，组成配置的dataID
    private final String flowDataId = APP_NAME + Constants.FLOW_POSTFIX;
    private final String paramDataId = APP_NAME + Constants.PARAM_FLOW_POSTFIX;
    private final String configDataId = "anypay-cluster-client-timeout-config";
    private final String serverDataId =  "anypay-cluster-server-base-config";


    @Override
    public void init() throws Exception {

        // Register client dynamic rule data source.
        //客户端，动态数据源的方式获取sentinel的流量控制规则
        initDynamicRuleProperty();

        // Register token client related data source.
        // Token client common config
        // 集群限流客户端的配置属性
        initClientConfigProperty();
        // Token client assign config (e.g. target token server) retrieved from assign map:
        //初始化Token客户端
        initClientServerAssignProperty();

        //初始化客户端状态
        initStateProperty();
    }

    /**
     * 加载client降级处理FlowRule
     * groupId +
     */
    private void initDynamicRuleProperty() {

        //流量控制的DataId分别是APP_NAME + Constants.FLOW_POSTFIX;热点参数限流规则的DataId是APP_NAME + Constants.PARAM_FLOW_POSTFIX;
        // client降级处理FlowRule动态加载
        ReadableDataSource<String, List<FlowRule>> ruleSource = new ZookeeperDataSource<>(remoteAddress, groupId,
                flowDataId, source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {}));
        FlowRuleManager.register2Property(ruleSource.getProperty());

    }

    /**
     * 指定requestTimeout时间
     */
    private void initClientConfigProperty() {
        ReadableDataSource<String, ClusterClientConfig> clientConfigDs = new ZookeeperDataSource<>(remoteAddress, groupId,
                configDataId, source -> JSON.parseObject(source, new TypeReference<ClusterClientConfig>() {}));
        ClusterClientConfigManager.registerClientConfigProperty(clientConfigDs.getProperty());
    }

    /**
     * 指定token-server的ip和端口
     */
    private void initClientServerAssignProperty() {
        ReadableDataSource<String, ClusterClientAssignConfig> clientAssignDs = new ZookeeperDataSource<>(remoteAddress, groupId,
                serverDataId, source -> JSON.parseObject(source, new TypeReference<ClusterClientAssignConfig>() {}));
        ClusterClientConfigManager.registerServerAssignProperty(clientAssignDs.getProperty());
    }

    /**
     * 表明当前服务为token-client
     */
    private void initStateProperty() {
        ClusterStateManager.applyState(ClusterStateManager.CLUSTER_CLIENT);

    }
}
