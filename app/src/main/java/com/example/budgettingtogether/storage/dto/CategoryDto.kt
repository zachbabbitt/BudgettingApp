package com.example.budgettingtogether.storage.dto

import com.example.budgettingtogether.categories.Category
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    @SerialName("name") val name: String,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("user_guid") val userGuid: String
) {
    fun toEntity(): Category = Category(
        name = name,
        isDefault = isDefault,
        userGuid = userGuid
    )

    companion object {
        fun fromEntity(c: Category) = CategoryDto(
            name = c.name,
            isDefault = c.isDefault,
            userGuid = c.userGuid
        )
    }
}
