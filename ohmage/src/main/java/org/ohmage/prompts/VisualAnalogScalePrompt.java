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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;

import org.ohmage.app.R;

import java.math.BigDecimal;

/**
 * Created by jaredsieling on 10/23/15.
 */
public class VisualAnalogScalePrompt extends AnswerablePrompt<BigDecimal> {
    public String minLabel;
    public String maxLabel;


    @Override
    public SurveyItemFragment getFragment() {

        return VisualAnalogScalePromptFragment.getInstance(this);
    }

    public static class VisualAnalogScalePromptFragment extends AnswerablePromptFragment<VisualAnalogScalePrompt> {

        TextView minLabelView;
        TextView maxLabelView;
        SeekBar slider;

        public static VisualAnalogScalePromptFragment getInstance(VisualAnalogScalePrompt prompt) {

            VisualAnalogScalePromptFragment fragment = new VisualAnalogScalePromptFragment();
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
            final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_visual_analog_scale, container, true);

            minLabelView = (TextView) view.findViewById(R.id.min_label);
            maxLabelView = (TextView) view.findViewById(R.id.max_label);
            slider = (SeekBar) view.findViewById(R.id.slider);

            if(getPrompt().minLabel != null){
                minLabelView.setText(getPrompt().minLabel);
            }
            if(getPrompt().maxLabel != null){
                maxLabelView.setText(getPrompt().maxLabel);
            }

            if(!isAnswered()) {
                // IMPORTANT, don't setValue again if this prompt has been answered. Otherwise,
                // if this method (onCreatePromptView) is called due to a notifyDatasetChanged(),
                // the setValue() will trigger another (recursive) notifyDatasetChanged() and the
                // app crashes.
                if (getPrompt().defaultResponse != null) {
                    slider.setProgress(getPrompt().defaultResponse.intValue());
                    setValue(getPrompt().defaultResponse.intValue());
                } else {
                    slider.setProgress(50);
                }
            }else if (getPrompt().value != null){
                slider.setProgress(getPrompt().value.intValue());
            }

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // nothing
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // nothing
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    setValue(seekBar.getProgress());
                }
            });

        }
    }
}
