package com.jesse.examination.score.route;

/** 用户成绩模块路由 URL 配置类。*/
public class ScoreServiceURL
{
    /** 用户成绩相关操作根 URI。*/
    final public static String SCORE_RECORD_ROOT_URI
        = "/api/score";

    final public static String SINGLE_SCORE_QUERY_URI
        = SCORE_RECORD_ROOT_URI + "/single_query";

    /** 分页查找指定用户的所有成绩记录 URI。*/
    final public static String PAGINATED_SCORE_QUERY_URI
        = SCORE_RECORD_ROOT_URI + "/paginated_query";

    /** 找出指定用户的最新一条成绩记录 URI。*/
    final public static String LATEST_SCORE_QUERY_URI
        = SCORE_RECORD_ROOT_URI + "/latest_query";

    /** 为指定用户插入一条新的成绩 URI。*/
    final public static String INSERT_NEW_SCORE_URI
        = SCORE_RECORD_ROOT_URI + "/insert";

    /** 删除指定用户对应的所有成绩，返回删除的行数 URI。*/
    final public static String DELETE_SCORE_URI
        = SCORE_RECORD_ROOT_URI + "/delete";

    /** 清空成绩表。*/
    final public static String TRUNCATE_SCORE_URI
        = SCORE_RECORD_ROOT_URI + "/truncate";
}
