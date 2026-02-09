package com.test.platform.observability.hub.web;

import com.test.platform.observability.hub.core.ObservedService;
import com.test.platform.observability.hub.core.ServiceSnapshot;
import com.test.platform.observability.hub.service.HubSnapshotService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HubController {
  private final HubSnapshotService snapshotService;

  public HubController(HubSnapshotService snapshotService) {
    this.snapshotService = snapshotService;
  }

  @GetMapping("/hub/services")
  public List<ObservedService> services() {
    return snapshotService.services();
  }

  @GetMapping("/hub/snapshot")
  public List<ServiceSnapshot> snapshot() {
    return snapshotService.snapshot();
  }
}

