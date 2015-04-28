/*
 * Copyright (C) 2014 ohmage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ohmage.prompts;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;

import org.ohmage.app.R;

import java.math.BigDecimal;

/**
 * Created by cketcham on 1/24/14.
 * TODO:can this be combined with textprompt? because the json is the same
 */
public class NumberPrompt extends AnswerablePrompt<BigDecimal> {
    public BigDecimal min;
    public BigDecimal max;
    public boolean wholeNumbersOnly;


    @Override
    public SurveyItemFragment getFragment() {

        return NumberPromptFragment.getInstance(this);
    }

    public static class NumberPromptFragment extends AnswerablePromptFragment<NumberPrompt> {

        private NumberPicker numberPicker;

        public static NumberPromptFragment getInstance(NumberPrompt prompt) {

            NumberPromptFragment fragment = new NumberPromptFragment();
            fragment.setPrompt(prompt);
            return fragment;
        }

        /**
         * Ensure the value is converted to BigDecimal before set it
         * (the super.setValue does not check this)
         * @param val
         */
        private void setValue(int val){
            super.setValue(new BigDecimal(val));
        }
        public void hideSoftKeyboard() {
            InputMethodManager
                    inputMethodManager = (InputMethodManager)  this.getActivity().getSystemService(
                    Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(this.getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        @Override
        public void onCreatePromptView(LayoutInflater inflater, final ViewGroup container,
                Bundle savedInstanceState) {
            final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_number, container, true);

            numberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
            numberPicker.requestFocus();
            if(getPrompt().max != null){
                numberPicker.setMaxValue(getPrompt().max.intValue());
            }
            if(getPrompt().min != null){
                numberPicker.setMinValue(getPrompt().min.intValue());
            }

            if(!isAnswered()) {
                // IMPORTANT, don't setValue again if this prompt has been answered. Otherwise,
                // if this method (onCreatePromptView) is called due to a notifyDatasetChanged(),
                // the setValue() will trigger another (recursive) notifyDatasetChanged() and the
                // app crashes.
                if (getPrompt().defaultResponse != null) {
                    numberPicker.setValue(getPrompt().defaultResponse.intValue());
                    setValue(getPrompt().defaultResponse.intValue());
                } else {
                    setValue(numberPicker.getValue());
                }
            }else if (getPrompt().value != null){
                numberPicker.setValue(getPrompt().value.intValue());
            }

            numberPicker.setOnValueChangedListener(new OnValueChangeListener() {
                @Override public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    setValue(newVal);
                }
            });
        }
    }
}
