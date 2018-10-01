package z.learn;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * spring integration of activiti
 * demo to programatically create an new flow
 *
 * 验证当前步骤，suspended状态时候execution的状态
 * select * from ACT_RU_EXECUTION;   execution有状态，当前Activity
 * active     SUSPENSION_STATE_ = 1
 * suspended  SUSPENSION_STATE_ = 2
 *
 * select * from ACT_RU_JOB;
 *
 * select * from ACT_HI_TASKINST;
 *
 * select * from ACT_RU_TASK;  task 是有状态的
 *
 * 步骤间流转通过 BpmnActivityBehavior 查找决定 take 那条路， FlowNodeActivityBehavior
 *
 * 验证点：暂停之后，异步设定的job是否会被结束，activity是否会被更新到新的步骤？？
 *
 * PROCESS_START
 * PROCESS_START_INITIAL
 * ACTIVITY_EXECUTE
 * TRANSITION_NOTIFY_LISTENER_END
 * TRANSITION_DESTROY_SCOPE
 * TRANSITION_NOTIFY_LISTENER_TAKE
 * TRANSITION_CREATE_SCOPE
 *                      ->TRANSITION_NOTIFY_LISTENER_START
 *                                  ->TRANSITION_CREATE_SCOPE
 *                                  ->ACTIVITY_EXECUTE
 */
public class Activiti {

    private static ApplicationContext context;
    private static RuntimeService runtimeService;
    private static TaskService taskService;
    private static RepositoryService repositoryService;

    public static void main(String[] args) throws InterruptedException {
        //使用类路径下面的XML资源来配置Spring应用上下文
        context = new ClassPathXmlApplicationContext("activiti.xml");
        runtimeService = context.getBean("runtimeService", RuntimeService.class);
        taskService = context.getBean("taskService", TaskService.class);
        repositoryService = context.getBean("repositoryService", RepositoryService.class);

        System.out.println("Number of process definitions: " + repositoryService.createProcessDefinitionQuery().count());

        exportBpmnFileByDefinitionKey(CreateFlow1.flowKey);
        exportBpmnFileByDefinitionKey(CreateFlow2.flowKey);
        exportBpmnFileByDefinitionKey(CreateFlow3.flowKey);

        //deployClasspathResource("VacationRequest.bpmn20.xml");
        //testSuspendProcessDefinitionByKey(CreateFlow1.flowKey);
        //testActivateProcessDefinitionByKey(CreateFlow1.flowKey);

        // 执行ｊｏｂ１的过程中会记录流程的ｒｅｖ，最后执行万去更新的时候如果版本不一致就会抛出并发更新异常
 /*       ProcessInstance instance = runtimeService.startProcessInstanceByKey("flow3");



        System.out.println(Thread.currentThread().getName() + " started:" + instance.getId());
        Thread.sleep(2 * 1000);

        taskService.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).list().forEach(t -> {
            // complete 是会同步去执行后面的serviceTask 需要改成完全异步的
            taskService.complete(t.getId());
            System.out.println(Thread.currentThread().getName() + " Complete task id: " + t.getId());
        });

        Thread.sleep(10 * 1000);

        // 暂停之后会更新ｅｘｅｃｕｔｉｏｎ的ｒｅｖ
        runtimeService.suspendProcessInstanceById(instance.getProcessInstanceId());
        // ｊｏｂ1执行完的时候去更新ｅｘｅｃｕｔｉｏｎ就会抛出并发更新异常，导致ｊｏｂ执行失败，所以会自动重试重新执行ｊｏｂ１
        System.out.println(Thread.currentThread().getName() + " suspended:" + instance.getId());
        Thread.sleep(30 * 1000);

        runtimeService.activateProcessInstanceById(instance.getProcessInstanceId());
        System.out.println(Thread.currentThread().getName() + " activated:" + instance.getId());
        Thread.sleep(10 * 60 * 1000);*/
    }

    public void viewImage() throws Exception {

    }

    private static void testSuspendProcessDefinitionByKey(String key) {
        /*多次调用无效：Cannot set suspension state 'suspended' for ProcessDefinitionEntity[flow1:1:5003]': already in state 'suspended'.*/
        System.out.println("suspendProcessDefinitionByKey");
        repositoryService.suspendProcessDefinitionByKey(key);
        try {

            System.out.println("startProcessInstanceByKey");
            runtimeService.startProcessInstanceByKey(key);
        } catch (ActivitiException e) {
            /*org.activiti.engine.ActivitiException: Cannot start process instance. Process definition first (id = flow1:1:5003) is suspended*/
            e.printStackTrace();
        }
    }

    private static void testActivateProcessDefinitionByKey(String key) {
        System.out.println("activateProcessDefinitionByKey");
        repositoryService.activateProcessDefinitionByKey(key);

        System.out.println("startProcessInstanceByKey");
        runtimeService.startProcessInstanceByKey(key);
    }

    private static void deployClasspathResource(String resource) {
        repositoryService.createDeployment().addClasspathResource(resource).deploy();

        System.out.println("Number of process definitions: " + repositoryService.createProcessDefinitionQuery().count());
    }

    private static void exportBpmnFileByDefinitionKey(String key) {
        repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).list()
                .forEach(processDefinition -> exportBpmnFileByDeploymentId(processDefinition.getDeploymentId()));
    }

    private static void exportBpmnFileByDeploymentId(String deploymentId) {
        List<String> names = repositoryService.getDeploymentResourceNames(deploymentId);

        String imageName = null;
        for (String name : names) {
            if (name.contains(".bpmn")) {
                imageName = name;
            }
        }

        if (imageName != null) {
            File f = new File("D:/JavaProject/activiti_demo/src/main/resources/bpmn/" + imageName);
            InputStream in = repositoryService.getResourceAsStream(deploymentId, imageName);
            try {
                FileUtils.copyInputStreamToFile(in, f);
            } catch (IOException e) {
                //
            }
        }
    }
}
