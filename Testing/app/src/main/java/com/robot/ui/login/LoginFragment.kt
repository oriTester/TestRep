package com.robot.ui.login

import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.text.TextWatcher
import android.text.Editable
import android.widget.Toast

import com.robot.R
import com.robot.activity.RobotActivity
import com.robot.application.MainApplication
import com.robot.ros.RosService
import com.robot.ui.main.MainFragment
import com.robot.utils.PreferencesManager
import com.robot.utils.hideKeyboard
import kotlinx.android.synthetic.main.login_fragment.*

class LoginFragment : Fragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()


        if(PreferencesManager.getString(PreferencesManager.USER_NAME) != null) {
            nameField.setText(PreferencesManager.getString(PreferencesManager.USER_NAME))
            //passField.setText("1234")
        }

        (requireActivity() as RobotActivity).supportActionBar!!.hide()

        connectButton.setOnClickListener {
            when {
                nameField.text.isNullOrEmpty() -> {
                    nameInputLayout.error = getString(R.string.name_empty)
                }
                passField.text.isNullOrEmpty() -> {
                    passInputLayout.error = getString(R.string.pass_empty)
                }
                else -> {
                    context?.hideKeyboard(passField)

                    RosService.instance.login(nameField.text.toString(),passField.text.toString()) { msg, success ->
                        if (success == true) {
                            PreferencesManager.putString(PreferencesManager.USER_NAME, nameField.text.toString())

                            activity?.supportFragmentManager?.beginTransaction()
                                ?.replace(R.id.container, MainFragment.newInstance())
                                ?.commitNow()
                        }
                        else if (msg!=null) {
                            Toast.makeText(
                                MainApplication.applicationContext(),
                                msg,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        nameField.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                nameInputLayout.error = null
            }
        })

        passField.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                passInputLayout.error = null
            }
        })

    }
}
