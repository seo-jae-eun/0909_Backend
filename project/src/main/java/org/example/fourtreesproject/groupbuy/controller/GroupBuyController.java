package org.example.fourtreesproject.groupbuy.controller;

import com.siot.IamportRestClient.exception.IamportResponseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;
import org.example.fourtreesproject.common.BaseResponse;
import org.example.fourtreesproject.common.BaseResponseStatus;
import org.example.fourtreesproject.exception.custom.InvalidGroupBuyException;
import org.example.fourtreesproject.exception.custom.InvalidUserException;
import org.example.fourtreesproject.groupbuy.model.request.GroupBuyCreateRequest;
import org.example.fourtreesproject.groupbuy.model.request.GroupBuySearchRequest;
import org.example.fourtreesproject.groupbuy.model.response.*;
import org.example.fourtreesproject.groupbuy.service.GroupBuyService;
import org.example.fourtreesproject.user.model.dto.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static org.example.fourtreesproject.common.BaseResponseStatus.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gpbuy")
public class GroupBuyController {

    private final GroupBuyService gpbuyService;

    @Operation(summary = "공구 등록api", description = "일반사용자가 마음에 드는 공구가 없을시 직접 공구를 등록<br><br>" + "※ 일반 회원 로그인이 필요한 기능입니다.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "Valid example", value = """
            {
                "categoryIdx": 2,
                "title": "sjb의 스프링 책 아무거나 대량으로 삽니다!",
                "content": "원가보다는 싸게 등록해주세요! 50개 한번에 사는 공구입니다!",
                "gpbuyQuantity": 50
              }""")})))

    @PostMapping("/register")
    public BaseResponse<GroupBuyRegisterResponse> register(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody GroupBuyCreateRequest request
            ){
        if (customUserDetails == null){
            throw new InvalidUserException(USER_NOT_LOGIN);
        }
        if (request == null){
            throw new InvalidGroupBuyException(GROUPBUY_REGIST_FAIL);
        }
        GroupBuyRegisterResponse groupBuyRegisterResponse = gpbuyService.save(customUserDetails.getUser().getIdx(), request);
        if (groupBuyRegisterResponse==null) {
            throw new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_REGIST_FAIL);
        }
        return new BaseResponse<>(groupBuyRegisterResponse);
    }


    @Operation(summary = "공구와 공구에 입찰한 입찰정보 조회 api",
            description = "일반사용자가 자신이 등록한 공구와 공구에 등록된 입찰 목록을 조회하는 기능입니다.<br><br>" + "※ 일반 회원 로그인이 필요한 기능입니다.")
    @GetMapping("/registered/bid/list")
    public BaseResponse<List<RegisteredBidListResponse>> registeredBidList(
            Long gpbuyIdx
    ) {
        RegisteredGroupBuyResponse result = gpbuyService.findBidList(gpbuyIdx);
//        if (result.getBidList().size() == 0) {
//            throw new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_LIST_REGISTERD_BID_EMPTY);
//        }
        if (gpbuyIdx == null) {
            throw new InvalidGroupBuyException(REQUEST_FAIL_INVALID);
        }

        return new BaseResponse<>(result);
    }

    @Operation(summary = "공구 전체 조회 api",
    description = "현재 진행중인 전체 공구 목록을 페이지 단위로 가져옵니다.<br><br>")
    @GetMapping("/list")
    public BaseResponse<List<GroupBuyListResponse>> list(
            Integer page, Integer size
    ){
        if (page == null){
            page = 0;
        }
        if (size == null || size == 0){
            size = 10;
        }
        List<GroupBuyListResponse> result = gpbuyService.list(page, size);
        if(result.size() == 0){
            throw new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_LIST_EMPTY);
        }

        return new BaseResponse<>(result);
    }

    @Operation(summary = "공구 상세 조회 api",
    description = "현재 진행중인 특정 공구에 대한 상세정보를 조회합니다.")
    @GetMapping("/detail")
    public BaseResponse<GroupBuyDetailResponse> detail(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Long gpbuyIdx
    ) {


        GroupBuyDetailResponse result = gpbuyService.findByGpbuyIdx(gpbuyIdx, customUserDetails);
        if (result == null) {
            throw new InvalidGroupBuyException(GROUPBUY_LIST_FAIL);
        }

        return new BaseResponse<>(result);

    }
    @Operation(summary = "관심 공구 등록/취소 api",
    description = "현재 진행중인 공구 중 하나를 로그인된 계정의 관심 공구 목록에 등록합니다. 다시 등록하면 취소됩니다.<br><br>" + "※ 일반 회원 로그인이 필요한 기능입니다.")
    @GetMapping("/likes/save")
    public BaseResponse<GroupBuyLikesResponse> likesSave(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Long gpbuyIdx
    ){
        if (customUserDetails == null){
            throw new InvalidUserException(USER_NOT_LOGIN);
        }
        return new BaseResponse<>(gpbuyService.likesSave(gpbuyIdx, customUserDetails.getIdx()));
    }

    //todo: list가 비어있으면 응답에서 result빼기
    @Operation(summary = "관심 공구 목록 조회 api",
    description = "현재 로그인된 계정에 등록된 관심 공구 목록을 조회합니다.<br><br>" + "※ 일반 회원 로그인이 필요한 기능입니다.")
    @GetMapping("/likes/list")
    public BaseResponse likesList(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        if (customUserDetails == null){
            throw new InvalidUserException(USER_NOT_LOGIN);
        }
        List<GroupBuyLikesListResponse> result = gpbuyService.likesList(customUserDetails.getIdx());
        if (result.size() == 0){
            throw new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_LIST_EMPTY);
        }
        return new BaseResponse(result);
    }

    @Operation(summary = "공구 검색 api",
    description = "현재 진행중인 공구 중, 입력한 조건에 맞는 공구를 검색합니다. 미 입력시 조건은 반영되지 않습니다.<br><br>")
    @GetMapping("/search")
    public BaseResponse search(
            @RequestParam(required = false) Long categoryIdx,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice    ) {

        GroupBuySearchRequest request = GroupBuySearchRequest.builder().page(page).size(size).categoryIdx(categoryIdx).minPrice(minPrice).maxPrice(maxPrice).build();
        List<GroupBuyListResponse> result = gpbuyService.search(request);
        if (result.size() == 0){
            throw new InvalidGroupBuyException(BaseResponseStatus.GROUPBUY_LIST_SEARCH_EMPTY);
        }

        return new BaseResponse<>(result);
    }

    @Operation(summary = "공구 취소 api",
    description = "현재 참여중인 공구를 취소합니다. 쿠폰,포인트의 사용내역이 원상복구되며 결제취소가 발생합니다.<br><br>" + "※ 일반 회원 로그인이 필요한 기능입니다.")
    @GetMapping("/cancle")
    public BaseResponse cancle(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Long ordersIdx
    ) throws IamportResponseException, IOException {
        if (gpbuyService.cancle(customUserDetails.getIdx(), ordersIdx)){
            throw new InvalidGroupBuyException(GROUPBUY_CANCLE_FAIL);
        };

        //Todo: 캔슬 리스폰스 작성
        return new BaseResponse();
    }

}
