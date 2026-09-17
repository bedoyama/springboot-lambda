package com.example.shortlink.persistence;

import com.example.shortlink.domain.Link;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("!local")
public class DynamoDbLinkRepository implements LinkRepository {

    private static final String CODE = "code";
    private static final String ORIGINAL_URL = "originalUrl";
    private static final String CREATED_AT = "createdAt";
    private static final String CLICK_COUNT = "clickCount";

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public DynamoDbLinkRepository(
            DynamoDbClient dynamoDb,
            @Value("${app.dynamodb.table-name}") String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    @Override
    public boolean create(Link link) {
        try {
            dynamoDb.putItem(request -> request
                    .tableName(tableName)
                    .item(toItem(link))
                    .conditionExpression("attribute_not_exists(#code)")
                    .expressionAttributeNames(Map.of("#code", CODE)));
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    @Override
    public Optional<Link> findByCode(String code) {
        GetItemResponse response = dynamoDb.getItem(request -> request
                .tableName(tableName)
                .key(key(code)));
        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromItem(response.item()));
    }

    @Override
    public Optional<Link> incrementClicks(String code) {
        try {
            UpdateItemResponse response = dynamoDb.updateItem(request -> request
                    .tableName(tableName)
                    .key(key(code))
                    .updateExpression("ADD clickCount :one")
                    .conditionExpression("attribute_exists(#code)")
                    .expressionAttributeNames(Map.of("#code", CODE))
                    .expressionAttributeValues(Map.of(":one", AttributeValue.fromN("1")))
                    .returnValues(ReturnValue.ALL_NEW));
            return Optional.of(fromItem(response.attributes()));
        } catch (ConditionalCheckFailedException e) {
            return Optional.empty();
        }
    }

    private static Map<String, AttributeValue> key(String code) {
        return Map.of(CODE, AttributeValue.fromS(code));
    }

    private static Map<String, AttributeValue> toItem(Link link) {
        return Map.of(
                CODE, AttributeValue.fromS(link.code()),
                ORIGINAL_URL, AttributeValue.fromS(link.originalUrl()),
                CREATED_AT, AttributeValue.fromS(link.createdAt().toString()),
                CLICK_COUNT, AttributeValue.fromN(Long.toString(link.clickCount())));
    }

    private static Link fromItem(Map<String, AttributeValue> item) {
        return new Link(
                item.get(CODE).s(),
                item.get(ORIGINAL_URL).s(),
                Instant.parse(item.get(CREATED_AT).s()),
                Long.parseLong(item.get(CLICK_COUNT).n()));
    }
}
