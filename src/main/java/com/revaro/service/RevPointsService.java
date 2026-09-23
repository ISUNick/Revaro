package com.revaro.service;

import com.revaro.entity.User;
import com.revaro.enums.RsvpStatus;
import com.revaro.repository.CommentLikeRepository;
import com.revaro.repository.CommentRepository;
import com.revaro.repository.EventRepository;
import com.revaro.repository.RsvpRepository;
import com.revaro.repository.UserCount;
import com.revaro.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RevPointsService {

    private static final int EVENT_POSTED = 5;
    private static final int GOING_RECEIVED = 2;
    private static final int INTERESTED_RECEIVED = 1;
    private static final int COMMENT_RECEIVED = 1;
    private static final int COMMENT_MADE = 1;
    private static final int LIKE_RECEIVED = 1;
    private static final int GOING_MADE = 1;

    // Older than any real data, so counting "since" this gives all-time totals
    private static final LocalDateTime ALL_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    public record RankedUser(User user, long score) {
    }

    public record Standing(long points, long rank) {
    }

    public RevPointsService(UserRepository userRepository,
                            EventRepository eventRepository,
                            RsvpRepository rsvpRepository,
                            CommentRepository commentRepository,
                            CommentLikeRepository commentLikeRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
    }

    // Keyed by user id. Users with no points aren't in the map.
    public Map<Long, Long> allTimePoints() {
        return pointsSince(ALL_TIME);
    }

    public Standing standingOf(User user) {
        Map<Long, Long> points = allTimePoints();
        long mine = points.getOrDefault(user.getId(), 0L);
        long ahead = points.values().stream().filter(p -> p > mine).count();
        return new Standing(mine, ahead + 1);
    }

    public List<RankedUser> topAllTime(int limit) {
        return rank(allTimePoints(), limit);
    }

    public List<RankedUser> topThisWeek(int limit) {
        return rank(pointsSince(LocalDateTime.now().minusWeeks(1)), limit);
    }

    public List<RankedUser> topOrganizers(int limit) {
        Map<Long, Long> eventCounts = new HashMap<>();
        add(eventCounts, eventRepository.countPostedByUser(ALL_TIME), 1);
        return rank(eventCounts, limit);
    }

    // Seven grouped queries total, no matter how many users there are
    private Map<Long, Long> pointsSince(LocalDateTime since) {
        Map<Long, Long> points = new HashMap<>();
        add(points, eventRepository.countPostedByUser(since), EVENT_POSTED);
        add(points, rsvpRepository.countReceivedByUser(RsvpStatus.GOING, since), GOING_RECEIVED);
        add(points, rsvpRepository.countReceivedByUser(RsvpStatus.INTERESTED, since), INTERESTED_RECEIVED);
        add(points, commentRepository.countReceivedByUser(since), COMMENT_RECEIVED);
        add(points, commentRepository.countMadeByUser(since), COMMENT_MADE);
        add(points, commentLikeRepository.countReceivedByUser(since), LIKE_RECEIVED);
        add(points, rsvpRepository.countMadeByUser(RsvpStatus.GOING, since), GOING_MADE);
        return points;
    }

    private void add(Map<Long, Long> points, List<UserCount> counts, int weight) {
        for (UserCount count : counts) {
            points.merge(count.getUserId(), count.getTotal() * weight, Long::sum);
        }
    }

    private List<RankedUser> rank(Map<Long, Long> scores, int limit) {
        List<Map.Entry<Long, Long>> top = scores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .toList();

        Map<Long, User> users = userRepository.findAllById(top.stream().map(Map.Entry::getKey).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return top.stream()
                .filter(entry -> users.containsKey(entry.getKey()))
                .map(entry -> new RankedUser(users.get(entry.getKey()), entry.getValue()))
                .toList();
    }
}
