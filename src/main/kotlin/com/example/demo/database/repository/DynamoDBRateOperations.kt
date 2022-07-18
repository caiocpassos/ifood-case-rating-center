package com.example.demo.database.repository

import com.example.demo.database.model.RateEntity
import com.example.demo.domain.adapter.AverageRepository
import com.example.demo.domain.adapter.RateRepository
import com.example.demo.domain.entity.Rate
import com.example.demo.domain.entity.Average
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional


@Repository
class DynamoDBRateOperations(
    private val dynamoDbRateTable: DynamoDbAsyncTable<RateEntity>,
    private val averageRepository: AverageRepository,
    private val conversionService: ConversionService
) : RateRepository {

    override suspend fun addRate(rate: Rate): Unit {
        val rateEntity = conversionService.convert(
            rate, RateEntity::class.java
        )
        dynamoDbRateTable.putItem(rateEntity).await()

        val averagePartitionKey = rate.merchantId
        averageRepository.saveAverage(averagePartitionKey, rate)
    }

    override suspend fun getRatesByMerchantId(merchantId: String): MutableList<Rate> {
        val key = Key.builder().partitionValue(merchantId).build()

        return dynamoDbRateTable
            .query(QueryConditional.keyEqualTo(key))
            .awaitLast()
            .items()
            .map { conversionService.convert(it, Rate::class.java)!! } as MutableList<Rate>
    }

    override suspend fun getMerchantAverage(merchantId: String): Average {
        return averageRepository.getMerchantAverage(merchantId)
    }
}