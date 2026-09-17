package com.example.shortlink.persistence;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;
import java.util.Optional;

public final class DynamoDbClickIncrement {

    private DynamoDbClickIncrement() {
    }

    public static Optional<Map<String, AttributeValue>> increment(
            DynamoDbClient client, String tableName, String code) {
        try {
            UpdateItemResponse response = client.updateItem(request -> request
                    .tableName(tableName)
                    .key(Map.of("code", AttributeValue.fromS(code)))
                    .updateExpression("ADD clickCount :one")
                    .conditionExpression("attribute_exists(#code)")
                    .expressionAttributeNames(Map.of("#code", "code"))
                    .expressionAttributeValues(Map.of(":one", AttributeValue.fromN("1")))
                    .returnValues(ReturnValue.ALL_NEW));
            return Optional.of(response.attributes());
        } catch (ConditionalCheckFailedException e) {
            return Optional.empty();
        }
    }
}
