package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.SignType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class LiveWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Recipient> recipients = new ArrayList<>();

    private Boolean allSignToComplete = false;

    @Enumerated(EnumType.STRING)
    private SignType signType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
    }

    public Boolean getAllSignToComplete() {
        return allSignToComplete;
    }

    public void setAllSignToComplete(Boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

    public SignType getSignType() {
        return signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }
}