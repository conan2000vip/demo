package com.healthlog.demo.service.home;

import java.util.Map;

public interface HomeService {

    Map<String, Object> getHomeData(Long profileId, Long currentUserId);

}