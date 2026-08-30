package com.semo.memo.data

import org.json.JSONArray
import org.json.JSONObject

/** Backup envelope version; bump when the JSON shape changes. */
const val BACKUP_SCHEMA_VERSION = 2

data class SemoBackup(
    val schemaVersion: Int,
    val exportedAt: Long,
    val memos: List<MemoEntity>,
    val bundles: List<BundleEntity>,
    val refs: List<BundleMemoCrossRef>,
)

object SemoBackupCodec {
    fun encode(backup: SemoBackup): String {
        val root = JSONObject()
            .put("schemaVersion", backup.schemaVersion)
            .put("exportedAt", backup.exportedAt)
            .put("memos", JSONArray().also { array ->
                backup.memos.forEach { memo ->
                    array.put(
                        JSONObject()
                            .put("id", memo.id)
                            .put("content", memo.content)
                            .put("createdAt", memo.createdAt)
                            .put("updatedAt", memo.updatedAt)
                            .put("isDeleted", memo.isDeleted),
                    )
                }
            })
            .put("bundles", JSONArray().also { array ->
                backup.bundles.forEach { bundle ->
                    array.put(
                        JSONObject()
                            .put("id", bundle.id)
                            .put("title", bundle.title)
                            .put("editableContent", bundle.editableContent)
                            .put("createdAt", bundle.createdAt)
                            .put("updatedAt", bundle.updatedAt)
                            .put("isPinned", bundle.isPinned)
                            .put("isArchived", bundle.isArchived),
                    )
                }
            })
            .put("refs", JSONArray().also { array ->
                backup.refs.forEach { ref ->
                    array.put(
                        JSONObject()
                            .put("bundleId", ref.bundleId)
                            .put("memoId", ref.memoId)
                            .put("addedAt", ref.addedAt)
                            .put("sortOrder", ref.sortOrder),
                    )
                }
            })
        return root.toString(2)
    }

    fun decode(json: String): SemoBackup {
        val root = JSONObject(json)
        require(root.has("schemaVersion")) { "스키마 버전이 없는 백업 파일입니다." }
        val schemaVersion = root.getInt("schemaVersion")
        require(schemaVersion in 1..BACKUP_SCHEMA_VERSION) {
            "지원하지 않는 백업 형식입니다. (schemaVersion=$schemaVersion)"
        }
        val memos = root.getJSONArray("memos").mapObjects { obj ->
            MemoEntity(
                id = obj.getLong("id"),
                content = obj.getString("content"),
                createdAt = obj.getLong("createdAt"),
                updatedAt = obj.getLong("updatedAt"),
                isDeleted = obj.optBoolean("isDeleted", false),
            )
        }
        val bundles = root.getJSONArray("bundles").mapObjects { obj ->
            BundleEntity(
                id = obj.getLong("id"),
                title = if (obj.isNull("title")) null else obj.getString("title"),
                editableContent = obj.getString("editableContent"),
                createdAt = obj.getLong("createdAt"),
                updatedAt = obj.getLong("updatedAt"),
                isPinned = obj.optBoolean("isPinned", false),
                isArchived = obj.optBoolean("isArchived", false),
            )
        }
        val refs = root.getJSONArray("refs").mapObjects { obj ->
            BundleMemoCrossRef(
                bundleId = obj.getLong("bundleId"),
                memoId = obj.getLong("memoId"),
                addedAt = obj.getLong("addedAt"),
                sortOrder = obj.getLong("sortOrder"),
            )
        }
        return SemoBackup(
            schemaVersion = schemaVersion,
            exportedAt = root.optLong("exportedAt", 0L),
            memos = memos,
            bundles = bundles,
            refs = refs,
        )
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        buildList(length()) {
            for (index in 0 until length()) add(transform(getJSONObject(index)))
        }
}
