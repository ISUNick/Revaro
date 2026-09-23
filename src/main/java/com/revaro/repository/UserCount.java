package com.revaro.repository;

// Row shape for the grouped count queries used by the Rev Points leaderboard
public interface UserCount {

    Long getUserId();

    Long getTotal();
}
