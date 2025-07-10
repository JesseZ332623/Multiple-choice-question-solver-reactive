package com.jesse.examination.question.route;

/** 问题查询模块路由 URL 配置类。 */
public class QuestionServiceURL
{
    /** 问题相关操作根 URI。*/
    final public static String QUESTION_ROOT_URI
        = "/api/question";

    /** 单条问题查询 URI。 */
    final public static String SINGLE_QUERY_URI
        = QUESTION_ROOT_URI + "/single_query";

    /** 所有问题完整信息 URI。 */
    final public static String PAGINATION_QUERY_URI
        = QUESTION_ROOT_URI + "/paginated_query";

    /** 所有问题加正确答案信息 URI。 */
    final public static String PAGINATION_QUERY_WITH_CORRECT_URI
        = QUESTION_ROOT_URI + "/paginated_query_with_correct";
}
