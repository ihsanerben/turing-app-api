package com.turing.app.api.notification.service;

import com.turing.app.api.notification.dto.NotificationDtos.CampaignRequest;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipationMailService {
  private final EmailCampaignService campaigns;

  public ParticipationMailService(EmailCampaignService campaigns) {
    this.campaigns = campaigns;
  }

  @Transactional
  public void enqueue(UUID user, String subject, String body) {
    var campaign =
        campaigns.create(user, new CampaignRequest(subject, body, Set.of(user), null, null), null);
    campaigns.send(user, campaign.id(), campaign.version(), null);
  }
}
