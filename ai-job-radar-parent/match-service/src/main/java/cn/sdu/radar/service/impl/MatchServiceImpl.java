package cn.sdu.radar.service.impl;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.feign.UserClient;
import cn.sdu.radar.mapper.MatchResultMapper;
import cn.sdu.radar.pojo.MatchResult;
import cn.sdu.radar.pojo.vo.MatchReportVO;
import cn.sdu.radar.service.MatchService;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import cn.sdu.radar.vo.UserProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements MatchService {
    private final UserClient userClient;
    private final JobClient jobClient;
    private final MatchResultMapper resultMapper;

    @Autowired
    public MatchServiceImpl(UserClient userClient, JobClient jobClient,
                            MatchResultMapper resultMapper) {
        this.userClient = userClient;
        this.jobClient = jobClient;
        this.resultMapper = resultMapper;
    }

    @Override
    public MatchReportVO match(Long userId, Long jobId) {
        UserProfileVO profile = requireData(userClient.getProfile(userId), "用户服务暂不可用");
        JobSummaryVO job = requireData(jobClient.getJob(jobId), "岗位服务暂不可用");

        Map<String, String> userSkills = skillMap(profile.getSkills());
        Map<String, String> requiredSkills = skillMap(job.getRequirements());
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> skill : requiredSkills.entrySet()) {
            if (userSkills.containsKey(skill.getKey())) {
                matched.add(skill.getValue());
            } else {
                missing.add(skill.getValue());
            }
        }

        int skillScore = requiredSkills.isEmpty()
                ? 60
                : (int) Math.round(60.0 * matched.size() / requiredSkills.size());
        int cityScore = sameText(profile.getCity(), job.getCity()) ? 15 : 0;
        int salaryScore = rangesOverlap(profile.getSalaryMin(), profile.getSalaryMax(),
                job.getSalaryMin(), job.getSalaryMax()) ? 15 : 0;
        int experienceScore = safe(profile.getExperienceYears()) >= safe(job.getExperienceYears())
                ? 10 : 0;
        int score = Math.min(100, skillScore + cityScore + salaryScore + experienceScore);

        MatchResult entity = new MatchResult();
        entity.setUserId(userId);
        entity.setJobId(jobId);
        entity.setScore(score);
        entity.setMatchedSkills(String.join(",", matched));
        entity.setMissingSkills(String.join(",", missing));
        entity.setSummary(summary(score, missing));
        entity.setCreatedAt(LocalDateTime.now());
        resultMapper.insert(entity);
        return toVO(entity, job);
    }

    @Override
    public List<MatchReportVO> history(Long userId) {
        return resultMapper.selectList(new QueryWrapper<MatchResult>()
                        .eq("user_id", userId)
                        .orderByDesc("created_at"))
                .stream()
                .map(result -> toVO(result,
                        requireData(jobClient.getJob(result.getJobId()), "岗位服务暂不可用")))
                .collect(Collectors.toList());
    }

    private <T> T requireData(CommonResult<T> result, String unavailableMessage) {
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException(503, unavailableMessage);
        }
        return result.getData();
    }

    private Map<String, String> skillMap(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(text)) {
            return result;
        }
        for (String part : text.split("[,，、;/；\\n]")) {
            String skill = part.trim();
            if (StringUtils.hasText(skill)) {
                result.put(skill.toLowerCase(Locale.ROOT), skill);
            }
        }
        return result;
    }

    private boolean sameText(String left, String right) {
        return StringUtils.hasText(left) && left.trim().equalsIgnoreCase(
                right == null ? "" : right.trim());
    }

    private boolean rangesOverlap(Integer expectedMin, Integer expectedMax,
                                  Integer offeredMin, Integer offeredMax) {
        return safe(expectedMin) <= safe(offeredMax) && safe(expectedMax) >= safe(offeredMin);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String summary(int score, List<String> missing) {
        String level = score >= 85 ? "匹配度很高，建议优先投递"
                : score >= 70 ? "整体匹配，可以针对性完善简历"
                : "存在一定能力差距，建议准备后再投递";
        if (missing.isEmpty()) {
            return level + "。核心技能要求已覆盖。";
        }
        return level + "。建议补充：" + String.join("、", missing) + "。";
    }

    private MatchReportVO toVO(MatchResult result, JobSummaryVO job) {
        MatchReportVO vo = new MatchReportVO();
        vo.setId(result.getId());
        vo.setScore(result.getScore());
        vo.setMatchedSkills(result.getMatchedSkills());
        vo.setMissingSkills(result.getMissingSkills());
        vo.setSummary(result.getSummary());
        vo.setCreatedAt(result.getCreatedAt());
        vo.setJob(job);
        return vo;
    }
}
