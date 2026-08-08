package com.moodi.discovery.application;

/**
 * 새로고침(커서 없는 요청)마다 새 시드를 발급한다. 시드가 바뀌면 셔플 순서가 바뀐다.
 */
public interface FeedSeedGenerator {

    String generate();
}
