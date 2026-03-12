package me.kuku.common.model

import me.kuku.common.entity.Status
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "baidu")
interface BaiduModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long
    val identityId: String
    val identityName: String
    val cookie: String
    val tieBaSToken: String?
    val sign: Status
}