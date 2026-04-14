package com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces;

import com.sni.bokaticowork.core.generator.sequenceEngine.enums.ResetPolicy;

import java.time.LocalDate;

public interface SequenceResetPolicyResolver{

   String resolveResetPolicy(ResetPolicy resetPolicy, LocalDate date);

}
