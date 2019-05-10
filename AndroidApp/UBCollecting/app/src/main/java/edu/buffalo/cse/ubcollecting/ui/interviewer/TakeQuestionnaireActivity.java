package edu.buffalo.cse.ubcollecting.ui.interviewer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;

import edu.buffalo.cse.ubcollecting.R;
import edu.buffalo.cse.ubcollecting.data.DatabaseHelper;
import edu.buffalo.cse.ubcollecting.data.models.Answer;
import edu.buffalo.cse.ubcollecting.data.models.QuestionPropertyDef;
import edu.buffalo.cse.ubcollecting.data.models.Questionnaire;
import edu.buffalo.cse.ubcollecting.data.models.QuestionnaireContent;
import edu.buffalo.cse.ubcollecting.data.models.Session;
import edu.buffalo.cse.ubcollecting.ui.QuestionManager;

import static edu.buffalo.cse.ubcollecting.ui.interviewer.TextFragment.SELECTED_ANSWER;
import static edu.buffalo.cse.ubcollecting.ui.interviewer.UserSelectQuestionnaireActivity.SELECTED_QUESTIONNAIRE;
import static edu.buffalo.cse.ubcollecting.ui.interviewer.UserSelectSessionActivity.SELECTED_SESSION;
import static edu.buffalo.cse.ubcollecting.ui.interviewer.ViewQuestionsActivity.QUESTION_INDEX;


/**
 * Landing activity that sets up the taking of a questionnaire
 */

public class TakeQuestionnaireActivity extends AppCompatActivity implements QuestionManager {

    private QuestionStatePagerAdapter questionStatePagerAdapter;
    private ViewPager questionViewPager;
    private ArrayList<QuestionnaireContent> questionnaire;
    public final static String QUESTIONNAIRE_CONTENT = "Question";
    public final static String IN_LOOP = "inLoop";
    public final static String QUESTION_TYPE="QuestionType";
    public final static String PARENT_ANSWER = "parentAnswer";
    public int questionIndex;
    private int loopIndex=0;
    private ArrayList<Answer> parentAnswers;
    private ArrayList<QuestionnaireContent> loopQuestions;
    private boolean inLoop;
    private int iterationsCounter = 0;
    private int currentQuestionPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_take_questionnaire);
        questionnaire = DatabaseHelper.QUESTIONNAIRE_CONTENT_TABLE.getAllQuestions(getQuestionnaire(getIntent()).getId());
        questionStatePagerAdapter = new QuestionStatePagerAdapter(getSupportFragmentManager());
        questionViewPager = findViewById(R.id.questionnaire_container);
        questionViewPager.setAdapter(questionStatePagerAdapter);
        questionIndex = (Integer) getIntent().getSerializableExtra(QUESTION_INDEX);
        getNextQuestion();
    }

    public void getNextQuestion(){
        QuestionnaireContent question;
        if(questionIndex>=questionnaire.size()){
            Toast.makeText(this, "You have successfully completed the questionnaire!", Toast.LENGTH_SHORT).show();
            Intent i = UserSelectQuestionnaireActivity.newIntent(TakeQuestionnaireActivity.this);
            i.putExtra(SELECTED_SESSION,getSession(getIntent()));
            startActivity(i);
            finish();

        }
        else{

            if(inLoop){
                if(loopIndex==loopQuestions.size()){
                    iterationsCounter++;
                    loopIndex=0;
                }
                Log.i("Looping", String.valueOf(loopIndex));
                question = loopQuestions.get(loopIndex);
                Log.i("QUESTION", String.valueOf(loopQuestions.size()));

            }
            else{
                question = questionnaire.get(questionIndex);

            }
            Bundle bundle = new Bundle();
            bundle.putSerializable(QUESTIONNAIRE_CONTENT,question);
            bundle.putSerializable(SELECTED_QUESTIONNAIRE, getQuestionnaire(getIntent()).getId());
            bundle.putSerializable(SELECTED_SESSION, getSession(getIntent()));
            bundle.putSerializable(IN_LOOP, inLoop);
            if(inLoop){
                bundle.putSerializable(PARENT_ANSWER, parentAnswers.get(iterationsCounter));
            }


            // get most recent answer(s)
            String questionId = question.getQuestionId();
            ArrayList<Answer> answerList = DatabaseHelper.ANSWER_TABLE.getMostRecentAnswer(questionId, getQuestionnaire(getIntent()).getId());

            // get type of question
            QuestionPropertyDef questionProperty = DatabaseHelper.QUESTION_PROPERTY_TABLE.getQuestionProperty(questionId);
            String typeOfQuestion = questionProperty.getName();

            bundle.putSerializable(QUESTION_TYPE,typeOfQuestion);

            if(!answerList.isEmpty()){
                bundle.putSerializable(SELECTED_ANSWER, answerList);
            }

            if(typeOfQuestion.equals("Audio")){
                AudioFragment audioFragment = new AudioFragment();
                audioFragment.setArguments(bundle);
                questionStatePagerAdapter.addFragement(audioFragment);
                questionStatePagerAdapter.notifyDataSetChanged();
            }
            else if(typeOfQuestion.equals("Video")){
                VideoFragment videoFragment = new VideoFragment();
                videoFragment.setArguments(bundle);
                questionStatePagerAdapter.addFragement(videoFragment);
                questionStatePagerAdapter.notifyDataSetChanged();
            }
            else if(typeOfQuestion.equals("Photo")){
                PhotoFragment photoFragment = new PhotoFragment();
                photoFragment.setArguments(bundle);
                questionStatePagerAdapter.addFragement(photoFragment);
                questionStatePagerAdapter.notifyDataSetChanged();
            }
            else if(typeOfQuestion.equals("List")){
                ListFragment listFragment = new ListFragment();
                listFragment.setArguments(bundle);
                questionStatePagerAdapter.addFragement(listFragment);
                questionStatePagerAdapter.notifyDataSetChanged();
            }
            else{
                Log.i("START", "FRAGMENT");

                TextFragment questionFragment = new TextFragment();
                questionFragment.setArguments(bundle);
                questionStatePagerAdapter.addFragement(questionFragment);
                questionStatePagerAdapter.notifyDataSetChanged();
            }

            questionViewPager.setCurrentItem(currentQuestionPosition);
            currentQuestionPosition+=1;



        }


    }

    public boolean isLastQuestion(){

            if(inLoop){
                loopIndex++;
                if(loopIndex==loopQuestions.size() &&iterationsCounter==parentAnswers.size()-1){
                    inLoop = false;
                    iterationsCounter = 0;
                    loopIndex = 0;
                }
            }
            else{
                questionIndex++;
            }
        return questionIndex-1 == questionnaire.size()-1;

    }

    public void startLoop(ArrayList<Answer> answers, String qcId){
        inLoop=true;
        parentAnswers = answers;
        loopQuestions = DatabaseHelper.QUESTIONNAIRE_CONTENT_TABLE.getLoopingQuestions(qcId);

        getNextQuestion();
    }

    public void saveAndQuitQuestionnaire(QuestionnaireContent questionnaireContent){
        Toast.makeText(this, "The questionnaire has been saved, you may resume it at any point", Toast.LENGTH_LONG).show();
        Intent i = UserSelectQuestionnaireActivity.newIntent(TakeQuestionnaireActivity.this);
        i.putExtra(SELECTED_SESSION , getSession(getIntent()));
        startActivity(i);
        finish();
    }

    /**
     * Helper function to extract a {@link edu.buffalo.cse.ubcollecting.data.models.Questionnaire} extra from and {@link Intent}
     * @param data {@link Intent} holding the extra
     * @return {@link edu.buffalo.cse.ubcollecting.data.models.Question} extra from {@link Intent}
     */
    public static Questionnaire getQuestionnaire(Intent data) {
        Serializable serializableObject = data.getSerializableExtra(SELECTED_QUESTIONNAIRE);

        return (Questionnaire) serializableObject;
    }

    public static Session getSession(Intent data) {
        Serializable serializableObject = data.getSerializableExtra(SELECTED_SESSION);

        return (Session) serializableObject;
    }

    public static Intent newIntent(Context packageContext) {
        Intent i = new Intent(packageContext, TakeQuestionnaireActivity.class);
        return i;
    }

    @Override
    public void onBackPressed() {
        Intent intent = UserLandingActivity.newIntent(TakeQuestionnaireActivity.this);
        startActivity(intent);
        finish();
    }


}


