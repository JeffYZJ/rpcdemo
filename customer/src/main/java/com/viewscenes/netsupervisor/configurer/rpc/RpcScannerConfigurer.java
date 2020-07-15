package com.viewscenes.netsupervisor.configurer.rpc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Rpc组件扫描配置工具类
 * 服务启动加载入口
 */
@Component
public class RpcScannerConfigurer implements BeanDefinitionRegistryPostProcessor {

    String basePackage = "com.viewscenes.netsupervisor.service";
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        //创建ClassPath扫描器，设置属性，然后调用扫描方法
        ClassPathRpcScanner scanner = new ClassPathRpcScanner(beanDefinitionRegistry);

        scanner.setAnnotationClass(null);
        //如果配置了annotationClass，就将其添加到includeFilters
        scanner.registerFilters();
        //扫描 service包下所有带有@Componment的组件
        scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }
}
