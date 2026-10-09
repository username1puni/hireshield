package com.hireshield.rules;

import org.springframework.stereotype.Component;

@Component
public class TaskScamRule extends PatternRule {

    public TaskScamRule() {
        super(
                "\\blike\\s+(youtube\\s+)?(videos?|posts?)\\b",
                "\\bsubscribe\\s+(to\\s+)?(youtube\\s+)?channels?\\b",
                "\\b(rate|review|rating)\\s+(hotels?|restaurants?|products?|movies?|apps?)\\b",
                "\\bgoogle\\s+(maps?\\s+)?(reviews?|ratings?)\\b",
                "\\b(prepaid|merchant|paid|vip)\\s+tasks?\\b",
                "\\b(daily|per\\s+task)\\s+commission\\b",
                "\\bearn\\b[^.\\n]{0,30}\\bper\\s+(task|like|review|video)\\b",
                "\\btelegram\\s+task\\b",
                "\\bclick\\s+['\"]?like\\b",
                "\\b(boost|increase)\\s+(their\\s+|the\\s+)?(popularity|views|ratings|video\\s+hits)\\b");
    }

    @Override
    public String code() {
        return "TASK_SCAM";
    }
}
