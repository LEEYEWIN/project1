package kr.fast.Jejuro.Service;


//[1페이지 여행 설문]

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.fast.Jejuro.Repository.CodeRepository;
import kr.fast.Jejuro.Repository.RegionRepository;
import kr.fast.Jejuro.Entity.Preference;
import kr.fast.Jejuro.Entity.PreferenceGroup;
import kr.fast.Jejuro.Entity.PreferenceOption;
import kr.fast.Jejuro.Repository.PreferenceOptionRepository;
import kr.fast.Jejuro.Repository.PreferenceRepository;
import kr.fast.Jejuro.ResponseDTO.TravelFormResponse;
import kr.fast.Jejuro.ResponseDTO.TravelFormResponse.CodeItem;
import kr.fast.Jejuro.ResponseDTO.TravelFormResponse.Option;
import kr.fast.Jejuro.ResponseDTO.TravelFormResponse.Question;
import kr.fast.Jejuro.ResponseDTO.TravelFormResponse.QuestionGroup;
import kr.fast.Jejuro.ResponseDTO.TravelFormResponse.RegionItem;

@Service
@Transactional(readOnly = true)
public class TravelFormService {

 private final RegionRepository regionRepository;
 private final CodeRepository codeRepository;
 private final PreferenceRepository preferenceRepository;
 private final PreferenceOptionRepository optionRepository;

 public TravelFormService(RegionRepository regionRepository, CodeRepository codeRepository,
                          PreferenceRepository preferenceRepository,
                          PreferenceOptionRepository optionRepository) {
     this.regionRepository = regionRepository;
     this.codeRepository = codeRepository;
     this.preferenceRepository = preferenceRepository;
     this.optionRepository = optionRepository;
 }

 public TravelFormResponse getForm() {
     List<RegionItem> regions = regionRepository.findAll().stream()
             .map(r -> new RegionItem(r.getRegionId(), r.getRegionName()))
             .toList();

     // 선택지를 질문 ID별로 묶는다
     Map<Long, List<Option>> optionsByQuestion = optionRepository
             .findAllByOrderByPreferenceIdAscOptionValueAsc().stream()
             .collect(Collectors.groupingBy(PreferenceOption::getPreferenceId, LinkedHashMap::new,
                     Collectors.mapping(o -> new Option(o.getOptionValue(), o.getOptionName(), o.getDescription()),
                             Collectors.toList())));

     // 질문을 그룹별로 묶는다 (질문 ID 순서 유지)
     Map<String, List<Preference>> questionsByGroup = preferenceRepository.findAllWithGroup().stream()
             .collect(Collectors.groupingBy(p -> p.getGroup().getGroupCode(), LinkedHashMap::new,
                     Collectors.toList()));

     List<QuestionGroup> groups = questionsByGroup.values().stream()
             .map(questions -> {
                 PreferenceGroup g = questions.get(0).getGroup();
                 List<Question> qs = questions.stream()
                         .map(p -> new Question(p.getPreferenceId(), p.getPreferenceCode(),
                                 p.getPreferenceName(), p.getResponseType().name(),
                                 optionsByQuestion.getOrDefault(p.getPreferenceId(), List.of())))
                         .toList();
                 return new QuestionGroup(g.getGroupCode(), g.getGroupName(),
                         g.getMinSelections(), g.getMaxSelections(), qs);
             })
             .toList();

     return new TravelFormResponse(regions, codes("TCR"), codes("GEN"), codes("AGE"), groups);
 }

 private List<CodeItem> codes(String groupCode) {
     return codeRepository.findByGroupCodeOrderByCodeId(groupCode).stream()
             .map(c -> new CodeItem(Integer.valueOf(c.getCodeValue()), c.getCodeName()))
             .toList();
 }
}