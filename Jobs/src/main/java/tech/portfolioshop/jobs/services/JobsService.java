package tech.portfolioshop.jobs.services;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import tech.portfolioshop.jobs.data.*;
import tech.portfolioshop.jobs.shared.JobsDto;

import javax.transaction.Transaction;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobsService {
    private final UserRepository userRepository;
    private final JobsRepository jobsRepository;
    private final SearchRepository searchRepository;


    public JobsService(UserRepository userRepository,
                       JobsRepository jobsRepository,
                       SearchRepository searchRepository) {
        this.userRepository = userRepository;
        this.jobsRepository = jobsRepository;
        this.searchRepository = searchRepository;
    }

    public List<JobsDto> findJobByQuery(String query, String location, String userId){
        UserEntity user = userRepository.getByUserId(userId);

        SearchEntity searchEntity = new SearchEntity();
        searchEntity.setQuery(query);
        searchEntity.setLocation(location);
        searchEntity.setUser(user);
        System.out.println(user.getId());

        searchRepository.save(searchEntity);

        return getJobsDtos(query, location);
    }

    private List<JobsDto> getJobsDtos(String query, String location) {
        List<JobsEntity> jobs = jobsRepository.findByQueryAndLocation(query,location);
        List<JobsDto> jobsDtos = new ArrayList<>();
        ModelMapper modelMapper = new ModelMapper();
        for(JobsEntity job : jobs){
            jobsDtos.add(modelMapper.map(job, JobsDto.class));
        }
        return jobsDtos;
    }

    public List<JobsDto> findJobByQuery(String query, String location){
        return getJobsDtos(query, location);
    }
    public List<JobsDto> findJobByRecommendation(String userId){
        List<JobsEntity> jobs = jobsRepository.findByRecommendation(userId);
        List<JobsDto> jobsDtos = new ArrayList<>();
        ModelMapper modelMapper = new ModelMapper();
        for(JobsEntity job : jobs){
            jobsDtos.add(modelMapper.map(job, JobsDto.class));
        }
        return jobsDtos;
    }
}
