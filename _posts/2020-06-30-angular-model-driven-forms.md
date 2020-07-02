---
layout: post
title: Model driven forms in Angular
date: '2020-06-30T00:00:00.000-00:00'
author: Stephen Nancekivell
tags: 
modified_time: '2020-06-30T00:00:00.000-00:00'
---

Model driven views are a great way to enforce consistency in any website or app. 

When making web forms there are lots of subtle descisions that need to be made about display, input validation, error handling and UX. By using a form component and a model driven view you can solve these issues in one place while still being able to handle a wide range of input types.

In this post I will talk about a form component I made for [Real Email](https://isitarealemail.com). In the login / user managemnet features alone there are 5 instances of this form.


![login form](/assets/2020-06-30-login-form.gif)


In [Angular](https://angular.io/) we will create a form component that accepts a simple model of the form and renders it.

Using the form will be as simple as.

```
<app-form [fields]="field" [onSubmit]="onSubmit.bind(this)">
</app-form>
```

```typescript
fields = {
    email: {
      label: 'Email',
      type: FieldModelType.Text,
      formControl: new FormControl('', [Validators.required]),
      errorMessage: 'Email is required',
    },
    password: {
      label: 'Password',
      type: FieldModelType.Password,
      formControl: new FormControl('', [Validators.required]),
      errorMessage: 'Password is required, it must be 8 characters and have a number.',
    },
};

const onSubmit = () => {
      return new Promise((resolve) => {
        console.log(
          fields.email.formControl.value,
          fields.password.formControl.value
        );
        setTimeout(resolve, 2000);
      });
};
```

The form model uses the [FromControl](https://angular.io/api/forms/FormControl) from angular reactive forms. This allows flexibility in form validation and access to the values in the submit function.

In the submit function you dont need to deal with any of the validation that is taken care of by the form component. And it is all done consistency.

To display validation error messages in this application we are taking the general approch of showing an error asking what is required. In your app you could take the approach of showing exactly what is wrong eg, 'Password is missing a number.'. This would require extending the errorMessage model.


The basic structure of the component loops over the input model and renders them in their different types. A submit button is at the bottom, which takes care of checking that the form is valid and putting the form into a loading state disabling the button to prevent double clicking.

```
<form [formGroup]="form" (ngSubmit)="submit()">
  <div *ngFor="let field of fieldModels">
    <div *ngIf="field.type === FieldModelType.Text">
    	..
	</div>
	<div *ngIf="field.type === FieldModelType.Password">
    	..
	</div>
  </div>
  <button
    type="submit"
    color="primary"
    disabled="{{ loading }}"
  >
    {{ submitLabel }}
  </button>
  ..
</form>

```


The complete component as used in my app Real Email is as bellow with using material UI styling.

```typescript
export enum FieldModelType {
  Text,
  Password,
}

export interface FieldModel {
  label: string;
  type: FieldModelType;
  formControl: FormControl;
  errorMessage?: string;
}

@Component({
  selector: 'app-form',
  templateUrl: './form.component.html',
  styleUrls: ['./form.component.css'],
})
export class FormComponent implements OnInit {
  form: FormGroup;
  @Input() onSubmit: () => Promise<void>;
  @Input() fields: { [key: string]: FieldModel };
  fieldModels: FieldModel[];
  @Input() submitLabel = 'Submit';
  loading = false;
  FieldModelType = FieldModelType;
  hide = true;

  ngOnInit(): void {
    const keys = Object.keys(this.fields);
    const z: { [key: string]: FormControl } = {};
    const controls = keys.reduce((acc, i) => {
      acc[i] = this.fields[i].formControl;
      return acc;
    }, z);

    this.fieldModels = keys.map((i) => this.fields[i]);

    this.form = new FormGroup(controls);
  }

  submit(): Promise<void> | undefined {
    this.form.markAsTouched();
    if (this.form.valid && !this.loading) {
      this.loading = true;
      const result = this.onSubmit();
      if (result) {
        return result.finally(() => (this.loading = false));
      } else {
        this.loading = false;
      }
    }
  }
  showError(fc: FormControl): boolean {
    return fc.invalid && (this.form.dirty || this.form.touched);
  }
}

```




```
<form [formGroup]="form" (ngSubmit)="submit()">
  <div *ngFor="let field of fieldModels">
    <div *ngIf="field.type === FieldModelType.Text">
      <mat-form-field appearance="outline">
        <mat-label>{{ field.label }}</mat-label>
        <input matInput [formControl]="field.formControl" />
        <mat-error *ngIf="showError(field.formControl)">{{
          field.errorMessage
        }}</mat-error>
      </mat-form-field>
    </div>
    <div *ngIf="field.type === FieldModelType.Password">
      <mat-form-field appearance="outline">
        <mat-label>{{ field.label }}</mat-label>
        <input
          matInput
          [type]="hide ? 'password' : 'text'"
          [formControl]="field.formControl"
        />
        <button
          mat-icon-button
          matSuffix
          (click)="hide = !hide"
          [attr.aria-label]="'Hide password'"
          [attr.aria-pressed]="hide"
        >
          <mat-icon>visibility</mat-icon>
        </button>
        <mat-error *ngIf="showError(field.formControl)">{{
          field.errorMessage
        }}</mat-error>
      </mat-form-field>
    </div>
  </div>
  <button
    mat-raised-button
    type="submit"
    color="primary"
    disabled="{{ loading }}"
  >
    {{ submitLabel }}
  </button>
  <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
</form>
```












