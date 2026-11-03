package com.aiproject.architect.modules.jobs.service;

import com.aiproject.architect.modules.jobs.dto.JobProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class JobProgressBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(JobProgressBroadcaster.class);
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String jobId) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout
        emitters.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(jobId, emitter));
        emitter.onTimeout(() -> removeEmitter(jobId, emitter));
        emitter.onError(e -> removeEmitter(jobId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(JobProgressEvent.builder()
                            .jobId(jobId)
                            .status("CONNECTED")
                            .progressPercent(0)
                            .currentStep("Realtime event connection established")
                            .build()));
        } catch (IOException e) {
            removeEmitter(jobId, emitter);
        }

        return emitter;
    }

    public void broadcast(String jobId, JobProgressEvent event) {
        List<SseEmitter> list = emitters.get(jobId);
        if (list == null || list.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("PROGRESS")
                        .data(event));
                if ("COMPLETED".equals(event.getStatus()) || "FAILED".equals(event.getStatus())) {
                    emitter.complete();
                }
            } catch (Exception e) {
                removeEmitter(jobId, emitter);
            }
        }
    }

    private void removeEmitter(String jobId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(jobId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(jobId);
            }
        }
    }
}
