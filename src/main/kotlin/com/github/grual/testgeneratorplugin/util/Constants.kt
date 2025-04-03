package com.github.grual.testgeneratorplugin.util

/* TODO these qualified names might depend on the version of the used framework
    consider making the frameworks selectable in the settings screen and dynamically
    loading the qualified names or smth similar
* */

// MOCK MVC
const val MOCK_MVC_GET = "org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get"
const val MOCK_MVC_POST = "org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post"
const val MOCK_MVC_DELETE = "org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete"
const val MOCK_MVC_PUT = "org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put"
const val MOCK_MVC_STATUS = "org.springframework.test.web.servlet.result.MockMvcResultMatchers.status"

// JUNIT
const val JUNIT_TEST_ANNOTATION = "org.junit.jupiter.api.Test"

// SPRING
const val SPRING_AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired"
const val SPRING_REQUEST_BODY_ANNOTATION = "org.springframework.web.bind.annotation.RequestBody"