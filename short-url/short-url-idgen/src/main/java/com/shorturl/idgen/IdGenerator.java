package com.shorturl.idgen;

/**
 * 发号器接口
 */
public interface IdGenerator {

    /**
     * 获取下一个ID
     */
    long nextId();

    /**
     * 初始化发号器（应用启动时调用）
     */
    void init();
}
