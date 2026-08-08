package io.github.easy4j.opencli.spi;

import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.registry.OpenCliAdapterIds;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.experimental.UtilityClass;

/**
 * 基于生成常量 {@link OpenCliAdapterIds#ALL} 的遍历工具，便于批处理或动态注册。
 */
@UtilityClass/**

 * Enumeration utility based on generated constants {@link OpenCliAdapterIds#ALL},
 * convenient for batch processing or dynamic registration.

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public class OpenCliAdapterEnumerator {

    /**
     * 对每个文档索引中的适配器 id 执行回调。
     *
     * @param consumer 消费 adapter id，不得为 null
     */
    public void forEachAdapterId(Consumer<String> consumer) {
        for (String id : OpenCliAdapterIds.ALL) {
            consumer.accept(id);
        }
    }

    /**
     * 为每个适配器创建 {@link OpenCliAdapterChannel} 并映射为自定义结果。
     *
     * @param channelFactory 由 adapter id 构造通道（通常委托 {@link io.github.easy4j.opencli.OpenCliClient#adapter(String)}）
     * @param mapper           将通道映射为业务对象
     * @param sink             消费映射结果
     * @param <R>              映射类型
     */
    public <R> void forEachChannel(
        Function<String, OpenCliAdapterChannel> channelFactory,
        Function<OpenCliAdapterChannel, R> mapper,
        Consumer<R> sink) {
        for (String id : OpenCliAdapterIds.ALL) {
            OpenCliAdapterChannel ch = channelFactory.apply(id);
            R r = mapper.apply(ch);
            sink.accept(r);
        }
    }
}
