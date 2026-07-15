package cn.sdu.radar.search;

import cn.sdu.radar.pojo.Job;
import cn.sdu.radar.vo.JobSummaryVO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "jobs")
public class JobDocument {
    @Id
    private Long id;
    @Field(type = FieldType.Text)
    private String title;
    @Field(type = FieldType.Text)
    private String company;
    @Field(type = FieldType.Keyword)
    private String city;
    @Field(type = FieldType.Integer)
    private Integer salaryMin;
    @Field(type = FieldType.Integer)
    private Integer salaryMax;
    @Field(type = FieldType.Integer)
    private Integer experienceYears;
    @Field(type = FieldType.Keyword)
    private String education;
    @Field(type = FieldType.Text)
    private String description;
    @Field(type = FieldType.Text)
    private String requirements;
    @Field(type = FieldType.Text)
    private String welfareTags;
    @Field(type = FieldType.Keyword)
    private String source;
    @Field(type = FieldType.Keyword, index = false)
    private String sourceUrl;
    @Field(type = FieldType.Keyword)
    private String status;
    @Field(type = FieldType.Long)
    private Long postedAtEpoch;

    public static JobDocument from(Job job) {
        JobDocument document = new JobDocument();
        document.setId(job.getId());
        document.setTitle(job.getTitle());
        document.setCompany(job.getCompany());
        document.setCity(job.getCity());
        document.setSalaryMin(job.getSalaryMin());
        document.setSalaryMax(job.getSalaryMax());
        document.setExperienceYears(job.getExperienceYears());
        document.setEducation(job.getEducation());
        document.setDescription(job.getDescription());
        document.setRequirements(job.getRequirements());
        document.setWelfareTags(job.getWelfareTags());
        document.setSource(job.getSource());
        document.setSourceUrl(job.getSourceUrl());
        document.setStatus(job.getStatus());
        document.setPostedAtEpoch(job.getPostedAt() == null ? 0L
                : java.sql.Timestamp.valueOf(job.getPostedAt()).getTime());
        return document;
    }

    public JobSummaryVO toSummary() {
        JobSummaryVO result = new JobSummaryVO();
        result.setId(id);
        result.setTitle(title);
        result.setCompany(company);
        result.setCity(city);
        result.setSalaryMin(salaryMin);
        result.setSalaryMax(salaryMax);
        result.setExperienceYears(experienceYears);
        result.setEducation(education);
        result.setDescription(description);
        result.setRequirements(requirements);
        result.setWelfareTags(welfareTags);
        result.setSource(source);
        result.setSourceUrl(sourceUrl);
        result.setStatus(status);
        if (postedAtEpoch != null && postedAtEpoch > 0) {
            result.setPostedAt(new java.sql.Timestamp(postedAtEpoch).toLocalDateTime());
        }
        return result;
    }
}
