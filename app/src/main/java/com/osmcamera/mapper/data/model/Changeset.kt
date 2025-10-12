package com.osmcamera.mapper.data.model

/**
 * OSM Changeset model
 */
data class Changeset(
    val id: Long,
    val open: Boolean = true,
    val createdAt: String? = null,
    val closedAt: String? = null,
    val user: String? = null,
    val uid: Long? = null,
    val minLat: Double? = null,
    val minLon: Double? = null,
    val maxLat: Double? = null,
    val maxLon: Double? = null,
    val commentsCount: Int = 0,
    val changesCount: Int = 0
)

/**
 * Changeset creation request
 */
data class CreateChangesetRequest(
    val comment: String,
    val source: String = "survey",
    val createdBy: String
)


