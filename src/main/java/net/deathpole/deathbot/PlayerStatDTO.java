package net.deathpole.deathbot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by nicolas on 04/10/17.
 */
public class PlayerStatDTO {

    private Integer id;

    private Long playerId;

    private Integer kl;

    private BigDecimal medals;

    private BigDecimal sr;

    private LocalDateTime updateDate;

    private String playerInstantName;

    private float srRatio;

    private BigDecimal srPercentage;

    public PlayerStatDTO() {
    }

    public PlayerStatDTO(Integer id, Long playerId, Integer kl, BigDecimal medals, BigDecimal sr, LocalDateTime updateDate, String playerInstantName, float srRatio,
            BigDecimal srPercentage) {
        this.id = id;
        this.playerId = playerId;
        this.kl = kl;
        this.medals = medals;
        this.sr = sr;
        this.updateDate = updateDate;
        this.playerInstantName = playerInstantName;
        this.srRatio = srRatio;
        this.srPercentage = srPercentage;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getKl() {
        return kl;
    }

    public void setKl(Integer kl) {
        this.kl = kl;
    }

    public BigDecimal getMedals() {
        return medals;
    }

    public void setMedals(BigDecimal medals) {
        this.medals = medals;
    }

    public BigDecimal getSr() {
        return sr;
    }

    public void setSr(BigDecimal sr) {
        this.sr = sr;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getPlayerInstantName() {
        return playerInstantName;
    }

    public void setPlayerInstantName(String playerInstantName) {
        this.playerInstantName = playerInstantName;
    }

    public float getSrRatio() {
        return srRatio;
    }

    public void setSrRatio(float srRatio) {
        this.srRatio = srRatio;
    }

    public BigDecimal getSrPercentage() {
        return srPercentage;
    }

    public void setSrPercentage(BigDecimal srPercentage) {
        this.srPercentage = srPercentage;
    }
}
