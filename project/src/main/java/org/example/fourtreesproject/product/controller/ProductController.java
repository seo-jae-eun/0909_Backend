package org.example.fourtreesproject.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;
import org.example.fourtreesproject.common.BaseResponse;
import org.example.fourtreesproject.file.FileUploadService;
import org.example.fourtreesproject.product.model.request.ProductRegisterRequest;
import org.example.fourtreesproject.product.model.response.ProductMylistResponse;
import org.example.fourtreesproject.product.model.response.ProductRegisterResponse;
import org.example.fourtreesproject.product.service.ProductService;
import org.example.fourtreesproject.user.model.dto.CustomUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.example.fourtreesproject.common.BaseResponseStatus.COMPANY_REGIST_FAIL;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final FileUploadService fileUploadService;

    @Operation(summary = "상품 등록 api", description = "업체 회원이 등록된 대기 중인 공구에 입찰을 등록 <br><br>" +
            "※ 업체 회원 로그인이 필요한 기능입니다.")
    @RequestMapping(method = RequestMethod.POST, value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<ProductRegisterResponse> register(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                          @RequestPart ProductRegisterRequest productInfo,
                                                          @RequestPart MultipartFile[] images) {
        if (customUserDetails.getUser().getRole().equals("ROLE_USER")) {
            return new BaseResponse<>(COMPANY_REGIST_FAIL); //업체회원이 아니면 예외처리
        }
        List<String> ImgUrlList = fileUploadService.upload(images); //경로를 productImgList 반환하고
        ProductRegisterResponse productResponse=  productService.register(customUserDetails.getUser(), productInfo, ImgUrlList);
        return new BaseResponse<>(productResponse);
    }

    @Operation(summary = "등록 상품 조회 api", description = "업체 회원 자신이 등록한 상품 조회 <br><br>" +
            "※ 업체 회원 로그인이 필요한 기능입니다.")
    @RequestMapping(method = RequestMethod.GET, value = "/mylist")
    public BaseResponse<List<ProductMylistResponse>> mylist(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails.getUser().getRole().equals("ROLE_USER")) {
            return new BaseResponse<>(COMPANY_REGIST_FAIL);
        }
        List<ProductMylistResponse> mylistResponse = productService.mylist(customUserDetails.getUser());
        return new BaseResponse<>(mylistResponse);
    }
}

