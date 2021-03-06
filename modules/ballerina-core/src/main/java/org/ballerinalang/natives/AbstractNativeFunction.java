/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/

package org.ballerinalang.natives;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.Annotation;
import org.ballerinalang.model.Function;
import org.ballerinalang.model.NativeUnit;
import org.ballerinalang.model.NodeLocation;
import org.ballerinalang.model.NodeVisitor;
import org.ballerinalang.model.ParameterDef;
import org.ballerinalang.model.SymbolName;
import org.ballerinalang.model.SymbolScope;
import org.ballerinalang.model.VariableDef;
import org.ballerinalang.model.statements.BlockStmt;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.SimpleTypeName;
import org.ballerinalang.model.values.BException;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.exceptions.ArgumentOutOfRangeException;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.ballerinalang.util.exceptions.FlowBuilderException;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code {@link AbstractNativeFunction}} represents a Abstract implementation of Native Ballerina Function.
 *
 * @since 0.8.0
 */
public abstract class AbstractNativeFunction implements NativeUnit, Function {

    /**
     * Value to be returned for functions having a void return
     */
    public static final BValue[] VOID_RETURN = new BValue[0];

    // BLangSymbol related attributes
    protected String name;
    protected String pkgPath;
    protected boolean isPublic = true;
    protected SymbolName symbolName;

    private List<Annotation> annotations;
    private List<ParameterDef> parameterDefs;
    private List<ParameterDef> returnParams;
    private int stackFrameSize;

    private BType[] returnParamTypes;
    private BType[] parameterTypes;
    private SimpleTypeName[] returnParamTypeNames;
    private SimpleTypeName[] argTypeNames;
    private String[] argNames;
    private int tempStackFrameSize;

    /**
     * Initialize a native function
     */
    public AbstractNativeFunction() {
        parameterDefs = new ArrayList<>();
        returnParams = new ArrayList<>();
        annotations = new ArrayList<>();
    }

    /**
     * Get Argument by index.
     *
     * @param context current {@code {@link Context}} instance.
     * @param index   index of the parameter.
     * @return BValue;
     */
    public BValue getArgument(Context context, int index) {
        if (index > -1 && index < argTypeNames.length) {
            BValue result = context.getControlStack().getCurrentFrame().values[index];
            if (result == null) {
                throw new BallerinaException("argument " + index + " is null");
            }
            return result;
        }
        throw new ArgumentOutOfRangeException(index);
    }

    /**
     * Where Native Function logic is implemented.
     *
     * @param context Current Context instance
     * @return Native function return BValue arrays
     */
    public abstract BValue[] execute(Context context);

    /**
     * Execute this native function and set the values for return parameters.
     *
     * @param context Ballerina Context
     */
    public void executeNative(Context context) {
        try {
            BValue[] retVals = execute(context);
            BValue[] returnRefs = context.getControlStack().getCurrentFrame().returnValues;
            if (returnRefs.length != 0) {
                for (int i = 0; i < returnRefs.length; i++) {
                    if (i < retVals.length) {
                        returnRefs[i] = retVals[i];
                    } else {
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            BException exception = new BException(e.getMessage());
            // TODO : Fix this once we remove Blocking executor
            if (context.getExecutor() != null) {
                context.getExecutor().handleBException(exception);
            } else {
                throw e;
            }
        }
    }

    /**
     * Util method to construct BValue arrays.
     *
     * @param values
     * @return BValue
     */
    public BValue[] getBValues(BValue... values) {
        return values;
    }

    // Methods in CallableUnit interface

    /**
     * Get all the Annotations associated with a BallerinaFunction.
     *
     * @return list of Annotations
     */
    @Override
    public Annotation[] getAnnotations() {
        return annotations.toArray(new Annotation[annotations.size()]);
    }

    /**
     * Get list of Arguments associated with the function definition.
     *
     * @return list of Arguments
     */
    public ParameterDef[] getParameterDefs() {
        return parameterDefs.toArray(new ParameterDef[parameterDefs.size()]);
    }

    /**
     * Get all the variableDcls declared in the scope of BallerinaFunction.
     *
     * @return list of all BallerinaFunction scoped variableDcls
     */
    @Override
    public VariableDef[] getVariableDefs() {
        return new VariableDef[0];
    }

    @Override
    public BlockStmt getCallableUnitBody() {
        return null;
    }

    @Override
    public ParameterDef[] getReturnParameters() {
        return returnParams.toArray(new ParameterDef[returnParams.size()]);
    }

    @Override
    public int getStackFrameSize() {
        return stackFrameSize;
    }

    @Override
    public void setStackFrameSize(int stackFrameSize) {
        this.stackFrameSize = stackFrameSize;
    }

    @Override
    public int getTempStackFrameSize() {
        return tempStackFrameSize;
    }

    @Override
    public void setTempStackFrameSize(int stackFrameSize) {
        if (this.tempStackFrameSize > 0 && stackFrameSize != this.tempStackFrameSize) {
            throw new FlowBuilderException("Attempt to Overwrite tempValue Frame size. current :" +
                    this.tempStackFrameSize + ", new :" + stackFrameSize);
        }
        this.tempStackFrameSize = stackFrameSize;
    }

    @Override
    public BType[] getReturnParamTypes() {
        return returnParamTypes;
    }

    @Override
    public void setReturnParamTypes(BType[] returnParamTypes) {
        this.returnParamTypes = returnParamTypes;
    }

    @Override
    public BType[] getArgumentTypes() {
        return parameterTypes;
    }

    @Override
    public void setParameterTypes(BType[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @Override
    public void accept(NodeVisitor visitor) {
    }

    // Methods in Node interface

    @Override
    public NodeLocation getNodeLocation() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }


    // Methods in BLangSymbol interface

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPackagePath() {
        return pkgPath;
    }

    @Override
    public void setPackagePath(String packagePath) {
        this.pkgPath = packagePath;
    }

    @Override
    public boolean isPublic() {
        return isPublic;
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public SymbolName getSymbolName() {
        return symbolName;
    }

    // Methods in NativeCallableUnit interface

    @Override
    public void setSymbolName(SymbolName symbolName) {
        this.symbolName = symbolName;
    }

    @Override
    public SymbolScope getSymbolScope() {
        return null;
    }

    @Override
    public void setArgTypeNames(SimpleTypeName[] argTypes) {
        this.argTypeNames = argTypes;
    }

    @Override
    public void setArgNames(String[] argNames) {
        this.argNames = argNames;
    }

    @Override
    public SimpleTypeName[] getArgumentTypeNames() {
        return argTypeNames;
    }

    @Override
    public String[] getArgumentNames() {
        return argNames;
    }

    @Override
    public SimpleTypeName[] getReturnParamTypeNames() {
        return returnParamTypeNames;
    }

    @Override
    public void setReturnParamTypeNames(SimpleTypeName[] returnParamTypes) {
        this.returnParamTypeNames = returnParamTypes;
    }
}
