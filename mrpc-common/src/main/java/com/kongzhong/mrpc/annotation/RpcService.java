package com.kongzhong.mrpc.annotation;


import com.kongzhong.mrpc.model.NoInterface;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC服务注解，标注服务实现类之上
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RpcService {

    /**
     * 服务实现的接口
     *
     * @return
     */
    Class<?> value() default NoInterface.class;

    /**
     * 当前服务版本号
     * @return
     */
    String version() default "";

    /**
     * 服务名
     * @return
     */
    String name() default "";

    /**
     * 服务所属APPID
     *
     * @return
     */
    String appId() default "";
}