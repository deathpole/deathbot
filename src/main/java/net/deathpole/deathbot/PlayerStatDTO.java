package net.deathpole.deathbot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by nicolas on 04/10/17.
 */
public class PlayerStatDTO {

    private Integer playerId;

    private Integer kl;

    private BigDecimal medals;

    private BigDecimal sr;

    private LocalDateTime updateDate;
    
    private String srMdodifier;

    public PlayerStatDTO() {
    }

    public PlayerStatDTO(Integer playerId, Integer kl, BigDecimal medals, BigDecimal sr, LocalDateTime updateDate) {
        this.playerId = playerId;
        this.kl = kl;
        this.medals = medals;
        this.sr = sr;
        this.updateDate = updateDate;
    }
    
    public PlayerStatDTO(Integer playerId, Integer kl, BigDecimal medals, BigDecimal sr, LocalDateTime updateDate, String srMdodifier) {
        this.playerId = playerId;
        this.kl = kl;
        this.medals = medals;
        this.sr = sr;
        this.updateDate = updateDate;
        this.srMdodifier = srMdodifier;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public Integer getKl() {
        return kl;
    }

    public void setKl(Integer kl) {
        this.kl = kl;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
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
    
    public String getSrModifier() {
        return srMdodifier;
    }

    public void setSrModifier(String srMdodifier) {
        this.srMdodifier = srMdodifier;
    }
}
