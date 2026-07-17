package com.relayflow.module.task.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.task.controller.app.vo.TaskListCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListIdReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberInviteReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberRemoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberUpdateRoleReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListUpdateReqVO;
import com.relayflow.module.task.service.list.TaskListService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/task/list")
public class TaskListController {

    private final TaskListService taskListService;

    @GetMapping("/mine")
    public CommonResult<List<TaskListRespVO>> mine() {
        return CommonResult.success(taskListService.listMine());
    }

    @GetMapping("/get")
    public CommonResult<TaskListRespVO> get(@RequestParam @NotNull Long id) {
        return CommonResult.success(taskListService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody TaskListCreateReqVO request) {
        return CommonResult.success(taskListService.create(request));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody TaskListUpdateReqVO request) {
        taskListService.update(request);
        return CommonResult.success(true);
    }

    @PostMapping("/archive")
    public CommonResult<Boolean> archive(@Valid @RequestBody TaskListIdReqVO request) {
        taskListService.archive(request);
        return CommonResult.success(true);
    }

    @GetMapping("/member/list")
    public CommonResult<List<TaskListMemberRespVO>> members(@RequestParam @NotNull Long listId) {
        return CommonResult.success(taskListService.listMembers(listId));
    }

    @PostMapping("/member/invite")
    public CommonResult<Boolean> invite(@Valid @RequestBody TaskListMemberInviteReqVO request) {
        taskListService.inviteMember(request);
        return CommonResult.success(true);
    }

    @PutMapping("/member/update-role")
    public CommonResult<Boolean> updateRole(@Valid @RequestBody TaskListMemberUpdateRoleReqVO request) {
        taskListService.updateMemberRole(request);
        return CommonResult.success(true);
    }

    @PostMapping("/member/remove")
    public CommonResult<Boolean> remove(@Valid @RequestBody TaskListMemberRemoveReqVO request) {
        taskListService.removeMember(request);
        return CommonResult.success(true);
    }
}
