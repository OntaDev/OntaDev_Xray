package com.ontadev.xray.domain;

import com.ontadev.libs.orm.annotation.entity.Column;
import com.ontadev.libs.orm.annotation.entity.Entity;
import com.ontadev.libs.orm.annotation.entity.GeneratedValue;
import com.ontadev.libs.orm.annotation.entity.Id;
import com.ontadev.libs.orm.annotation.entity.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table("materials")
public class MaterialEntry {
    @Id
    @GeneratedValue
    private Integer id;

    @Column(nullable = false)
    private Material material;

    @Column(nullable = false)
    private ChatColor color;
}
