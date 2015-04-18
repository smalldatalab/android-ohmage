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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import org.ohmage.app.R;

/**
 * This fragment is the base class for a survey item. All fragments in the
 * {@link org.ohmage.app.SurveyActivity.PromptFragmentAdapter} will be an instance of this class.
 */
public class SurveyItemFragment extends Fragment {

    private View.OnClickListener mOnClickListener;
    private View mButtons;
    private TextView skipButton;
    private TextView okButton;

    private boolean mAnswered = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.prompt_basic, container, false);
        ((TextView) view.findViewById(R.id.text)).setText(getPromptText());
        mButtons = view.findViewById(R.id.buttons);
        skipButton = (TextView) view.findViewById(R.id.skip);
        okButton = (TextView) view.findViewById(R.id.ok);

        okButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mAnswered = true;
                onOkPressed();
            }
        });
        skipButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mAnswered = true;
                onSkipPressed();
            }
        });

        if(isSkippable()) {
            skipButton.setVisibility(View.VISIBLE);
        } else {
            skipButton.setVisibility(View.GONE);
        }

        if (isAnswered()) {
            mButtons.setVisibility(View.GONE);
        } else {
            updateCanContinue();
        }

        onCreatePromptView(inflater, (ViewGroup) view.findViewById(R.id.content),
                savedInstanceState);

        // Fade the prompt in
        if (!isAnswered()) {
            AlphaAnimation aa = new AlphaAnimation(0f, 1f);
            aa.setFillAfter(true);
            aa.setDuration(200);
            view.startAnimation(aa);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.setOnClickListener(mOnClickListener);
    }

    protected void onSkipPressed() {
        // Sub-classes should handle the skip pressed event
    }

    public void dispatchSkipPressed() {
        skipButton.performClick();
    }

    protected void onOkPressed() {
        // Sub-classes should handle the ok pressed event
    }

    public void dispatchOkPressed() {
        okButton.performClick();
    }

    /**
     * Sub-classes should return the text to show for this prompt if they don't create their
     * @return
     */
    protected String getPromptText() {
        return "";
    }

    protected boolean canContinue() {
        return true;
    }

    public boolean isAnswered() {
        return mAnswered;
    }

    protected void updateCanContinue() {
        if(canContinue()) {
            okButton.setEnabled(true);
        } else {
            okButton.setEnabled(false);
        }
    }

    protected boolean isSkippable() {
        return true;
    }

    /**
     * Allow children to inflate their subview into the main view.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     */
    public void onCreatePromptView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public void showButtons(int visibility) {
        mAnswered = visibility == View.GONE;
        if(mButtons != null)
            mButtons.setVisibility(visibility);
    }
    public void resetAnswerState(){
        mAnswered = false;
    }
}
