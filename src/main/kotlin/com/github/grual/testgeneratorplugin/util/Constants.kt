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
const val SPRING_GET_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.GetMapping"
const val SPRING_POST_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.PostMapping"
const val SPRING_PUT_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.PutMapping"
const val SPRING_DELETE_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.DeleteMapping"
// JAVA
const val NOT_NULL_ANNOTATION = "javax.validation.constraints.NotNull"

// Lombok
const val SETTER_ANNOTATION = "lombok.Setter"
const val BUILDER_ANNOTATION = "lombok.Builder"
const val SUPER_BUILDER_ANNOTATION = "lombok.experimental.SuperBuilder"

// CUSTOM
const val NO_VALUE_PARAMETER = "/* TODO provide value */"