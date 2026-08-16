package com.example.springlab.item;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Item 機能の統合テスト。
 *
 * <p>CRUD 系は {@link Transactional} を付けてテスト後に自動ロールバックし、開発DBを汚さない。 トランザクションのロールバック挙動そのものの検証は {@link
 * ItemTransactionRollbackTest}（別クラス）で行う。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createReturns201WithDefaultStatusAndTimestamps() throws Exception {
        mockMvc
                .perform(
                        post("/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"商品A\",\"price\":1000,\"description\":\"説明\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("商品A"))
                .andExpect(jsonPath("$.price").value(1000))
                .andExpect(jsonPath("$.status").value("ON_SALE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createWithoutDescriptionSucceeds() throws Exception {
        mockMvc
                .perform(
                        post("/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"説明なし\",\"price\":500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void createWithInvalidPriceReturns400() throws Exception {
        mockMvc
                .perform(
                        post("/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"x\",\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createWithTooLongNameReturns400() throws Exception {
        String longName = "あ".repeat(41);
        mockMvc
                .perform(
                        post("/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"" + longName + "\",\"price\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturnsCreatedItem() throws Exception {
        String body =
                mockMvc
                        .perform(
                                post("/items")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"取得対象\",\"price\":300}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = extractId(body);

        mockMvc
                .perform(get("/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("取得対象"));
    }

    @Test
    void getByMissingIdReturns404() throws Exception {
        mockMvc
                .perform(get("/items/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("商品が存在しません"));
    }

    @Test
    void getByNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/items/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void updateStatusChangesToSoldOut() throws Exception {
        String body =
                mockMvc
                        .perform(
                                post("/items")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"更新対象\",\"price\":800}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = extractId(body);

        mockMvc
                .perform(put("/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD_OUT"));
    }

    @Test
    void updateMissingItemReturns404() throws Exception {
        mockMvc.perform(put("/items/{id}", 999999)).andExpect(status().isNotFound());
    }

    @Test
    void deleteExistingReturns204ThenMissingReturns404() throws Exception {
        String body =
                mockMvc
                        .perform(
                                post("/items")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"削除対象\",\"price\":100}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = extractId(body);

        mockMvc.perform(delete("/items/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/items/{id}", id)).andExpect(status().isNotFound());
    }

    private static long extractId(String json) {
        int i = json.indexOf("\"id\":") + 5;
        int j = i;
        while (j < json.length() && Character.isDigit(json.charAt(j))) {
            j++;
        }
        return Long.parseLong(json.substring(i, j));
    }
}
