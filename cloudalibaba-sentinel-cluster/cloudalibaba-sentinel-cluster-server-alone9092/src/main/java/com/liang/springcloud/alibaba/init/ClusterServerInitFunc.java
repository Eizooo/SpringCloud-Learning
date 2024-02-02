package com.liang.springcloud.alibaba.init;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterParamFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerFlowConfig;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.zookeeper.ZookeeperDataSource;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.liang.springcloud.alibaba.Constants;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @PROJECT_NAME: SpringCloud-Learning
 * @USER: yuliang
 * @DESCRIPTION:
 * @DATE: 2021-04-01 10:01
 */
public class ClusterServerInitFunc implements InitFunc {

    //nacos集群地址
    private final String remoteAddress = "127.0.0.1:2181";
    //配置的分组名称
    private final String groupId = "sentinel_pay_group";

    //配置的dataId
    private final String namespaceSetDataId = "cluster-server-namespace-set";
    private final String serverTransportDataId = "cluster-server-transport-config";
    private final String serverFlowDataId = "pay-cluster-server-flow-config";

    @Override
    public void init() {

        //这标识监听哪些namespace（集群的namespace或客户端项目名）下的集群限流规则
        //namespace的意义?一般为app.name, 不同token_server客户端读取不同的目录?
        //避免其他token_server读取到了相同的配置数据,但是groupId也有类似的功能
        //可以理解为同一个Group里面可以有多个集群应用, 多个token_server
        //dashBoard如何动态加载groupId? 需要进行改造, 要么就是在zookeeper里面配置一个groupID : app.name的 键值对
        initPropertySupplier();
        // 设置tokenServer管辖的作用域(即管理哪些应用), 配置了这个才会在上一个PropertySupplier中进行监听
        initTokenServerNameSpaces();

        // Server transport configuration data source.
        //Server端配置. 端口, 不配置默认为 18730,600
        initServerTransportConfig();

        //初始化token-server最大qps
        initServerFlowConfig();

        //初始化服务器状态,设定为token-server
        initStateProperty();

    }

    private  void initPropertySupplier(){

        // Register cluster flow rule property supplier which creates data source by namespace.
        // Flow rule dataId format: ${namespace}-flow-rules
        ClusterFlowRuleManager.setPropertySupplier(namespace -> {
            ReadableDataSource<String, List<FlowRule>> ds = new ZookeeperDataSource<>(remoteAddress, groupId,
                    namespace + Constants.FLOW_POSTFIX,
                    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {}));
            return ds.getProperty();
        });

    }


    private void initTokenServerNameSpaces(){
        // Server namespace set (scope) data source.
        ReadableDataSource<String, Set<String>> namespaceDs = new ZookeeperDataSource<>(remoteAddress, groupId,
                namespaceSetDataId, source -> JSON.parseObject(source, new TypeReference<Set<String>>() {}));
        ClusterServerConfigManager.registerNamespaceSetProperty(namespaceDs.getProperty());
    }

    private void initServerTransportConfig(){
        // Server transport configuration data source.
        ReadableDataSource<String, ServerTransportConfig> transportConfigDs = new ZookeeperDataSource<>(remoteAddress,
                groupId, serverTransportDataId,
                source -> JSON.parseObject(source, new TypeReference<ServerTransportConfig>() {}));
        ClusterServerConfigManager.registerServerTransportProperty(transportConfigDs.getProperty());
    }


    private void initServerFlowConfig(){

        // Server namespace set (scope) data source.
        ReadableDataSource<String, ServerFlowConfig> serverFlowConfig = new ZookeeperDataSource<>(remoteAddress, groupId,
                serverFlowDataId, source -> JSON.parseObject(source, new TypeReference<ServerFlowConfig>() {}));

        ClusterServerConfigManager.registerGlobalServerFlowProperty(serverFlowConfig.getProperty());
    }

    private void initStateProperty() {
        ClusterStateManager.applyState(ClusterStateManager.CLUSTER_SERVER);

    }
}
