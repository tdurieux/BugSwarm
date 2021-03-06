"""Matrix factorization with Sparse PCA"""
# Author: Vlad Niculae, Gael Varoquaux, Alexandre Gramfort
# License: BSD 3 clause

import warnings

import numpy as np

from ..utils import check_random_state, check_array
from ..utils.validation import check_is_fitted
from ..linear_model import ridge_regression
from ..base import BaseEstimator, TransformerMixin
from .dict_learning import dict_learning, dict_learning_online


class SparsePCA(BaseEstimator, TransformerMixin):
    """Sparse Principal Components Analysis (SparsePCA)

    Finds the set of sparse components that can optimally reconstruct
    the data.  The amount of sparseness is controllable by the coefficient
    of the L1 penalty, given by the parameter alpha.

    Read more in the :ref:`User Guide <SparsePCA>`.

    Parameters
    ----------
    n_components : int,
        Number of sparse atoms to extract.

    alpha : float,
        Sparsity controlling parameter. Higher values lead to sparser
        components.

    ridge_alpha : float,
        Amount of ridge shrinkage to apply in order to improve
        conditioning when calling the transform method.

    max_iter : int,
        Maximum number of iterations to perform.

    tol : float,
        Tolerance for the stopping condition.

    method : {'lars', 'cd'}
        lars: uses the least angle regression method to solve the lasso problem
        (linear_model.lars_path)
        cd: uses the coordinate descent method to compute the
        Lasso solution (linear_model.Lasso). Lars will be faster if
        the estimated components are sparse.

    n_jobs : int,
        Number of parallel jobs to run.

    U_init : array of shape (n_samples, n_components),
        Initial values for the loadings for warm restart scenarios.

    V_init : array of shape (n_components, n_features),
        Initial values for the components for warm restart scenarios.

    verbose : int
        Controls the verbosity; the higher, the more messages. Defaults to 0.

    random_state : int, RandomState instance or None, optional (default=None)
        If int, random_state is the seed used by the random number generator;
        If RandomState instance, random_state is the random number generator;
        If None, the random number generator is the RandomState instance used
        by `np.random`.

    normalize_components : boolean, optional (default=False)
        - if False, use a version of Sparse PCA without components
          normalization and without data centering. This is likely a bug and
          even though it's the default for backward compatibility,
          this should not be used.
        - if True, use a version of Sparse PCA with components normalization
          and data centering.

        .. versionadded:: 0.20

        .. deprecated:: 0.22
           ``normalize_components`` was added and set to ``False`` for
           backward compatibility. It would be set to ``True`` from 0.22
           onwards.

    compute_variance : bool, optional (default=True)
        If true, this will compute the explained variance versus component.

    Attributes
    ----------
    components_ : array, shape (n_components, n_features)
        Sparse components extracted from the data.

    error_ : array
        Vector of errors at each iteration.

    n_iter_ : int
        Number of iterations run.

    mean_ : array, shape (n_features, )
        Per-feature empirical mean, estimated from the training set.
        Equal to ``X.mean(axis=0)``.

    explained_variance_ : array, shape (n_components, )
        The explained variance versus component
        Only computed if variance is set to True

    See also
    --------
    PCA
    MiniBatchSparsePCA
    DictionaryLearning
    """
    def __init__(self, n_components=None, alpha=1, ridge_alpha=0.01,
                 max_iter=1000, tol=1e-8, method='lars', n_jobs=1, U_init=None,
                 V_init=None, verbose=False, random_state=None,
                 normalize_components=False, compute_variance=True):
        self.n_components = n_components
        self.compute_variance = compute_variance
        self.alpha = alpha
        self.ridge_alpha = ridge_alpha
        self.max_iter = max_iter
        self.tol = tol
        self.method = method
        self.n_jobs = n_jobs
        self.U_init = U_init
        self.V_init = V_init
        self.verbose = verbose
        self.random_state = random_state
        self.normalize_components = normalize_components

    def fit(self, X, y=None):
        """Fit the model from data in X.

        Parameters
        ----------
        X : array-like, shape (n_samples, n_features)
            Training vector, where n_samples in the number of samples
            and n_features is the number of features.

        y : Ignored

        Returns
        -------
        self : object
            Returns the instance itself.
        """
        random_state = check_random_state(self.random_state)
        X = check_array(X)

        if self.normalize_components:
            self.mean_ = X.mean(axis=0)
            X = X - self.mean_
        else:
            warnings.warn("normalize_components=False is a "
                          "backward-compatible setting that implements a "
                          "non-standard definition of sparse PCA. This "
                          "compatibility mode will be removed in 0.22.",
                          DeprecationWarning)

        if self.n_components is None:
            n_components = X.shape[1]
        else:
            n_components = self.n_components
        code_init = self.V_init.T if self.V_init is not None else None
        dict_init = self.U_init.T if self.U_init is not None else None
        Vt, _, E, self.n_iter_ = dict_learning(X.T, n_components, self.alpha,
                                               tol=self.tol,
                                               max_iter=self.max_iter,
                                               method=self.method,
                                               n_jobs=self.n_jobs,
                                               verbose=self.verbose,
                                               random_state=random_state,
                                               code_init=code_init,
                                               dict_init=dict_init,
                                               return_n_iter=True
                                               )
        self.components_ = Vt.T

        # compute variance before normalizing components (if both are desired)
        if self.compute_variance:
            self.explained_variance_ = self._get_explained_variance(X)
        else:
            self.explained_variance_ = None

        if self.normalize_components:
            components_norm = \
                    np.linalg.norm(self.components_, axis=1)[:, np.newaxis]
            components_norm[components_norm == 0] = 1
            self.components_ /= components_norm

        self.error_ = E
        return self

    def transform(self, X, ridge_alpha='deprecated'):
        """Least Squares projection of the data onto the sparse components.

        To avoid instability issues in case the system is under-determined,
        regularization can be applied (Ridge regression) via the
        `ridge_alpha` parameter.

        Note that Sparse PCA components orthogonality is not enforced as in PCA
        hence one cannot use a simple linear projection.

        Parameters
        ----------
        X : array, shape (n_samples, n_features)
            Test data to be transformed, must have the same number of
            features as the data used to train the model.

        ridge_alpha : float, default: 0.01
            Amount of ridge shrinkage to apply in order to improve
            conditioning.

            .. deprecated:: 0.19
               This parameter will be removed in 0.21.
               Specify ``ridge_alpha`` in the ``SparsePCA`` constructor.

        Returns
        -------
        X_new array, shape (n_samples, n_components)
            Transformed data.
        """
        check_is_fitted(self, 'components_')

        X = check_array(X)
        if ridge_alpha != 'deprecated':
            warnings.warn("The ridge_alpha parameter on transform() is "
                          "deprecated since 0.19 and will be removed in 0.21. "
                          "Specify ridge_alpha in the SparsePCA constructor.",
                          DeprecationWarning)
            if ridge_alpha is None:
                ridge_alpha = self.ridge_alpha
        else:
            ridge_alpha = self.ridge_alpha

        if self.normalize_components:
            X = X - self.mean_

        U = ridge_regression(self.components_.T, X.T, ridge_alpha,
                             solver='cholesky')

        if not self.normalize_components:
            s = np.sqrt((U ** 2).sum(axis=0))
            s[s == 0] = 1
            U /= s

        return U

    def _get_explained_variance(self, X):
        '''
            Get the explained variance from the principal components of the
            data.

            Parameters
            ----------
            X : ndarray, shape (n_samples, n_features)
                The feature vector. n_samples and n_features are the number of
                samples and features, respectively.

            Notes
            -----
            The variance ratio may not be computed. The main reason is that we
            do not know what the total variance is since we did not compute all
            the components.
        '''
        components = self.components_
        # requires the mean to be subtracted
        if not self.normalize_components:
            # should be removed in version 0.22
            X = X - np.mean(X, axis=0)
        # the number of samples
        n_samples = X.shape[0]
        n_components = components.shape[0]
        unit_vecs = components.copy()
        components_norm = \
            np.linalg.norm(components, axis=1)[:, np.newaxis]
        components_norm[components_norm == 0] = 1
        unit_vecs /= components_norm

        # Algorithm, as we compute the variance for each projection, we
        # subtract the variance from components in the direction of previous
        # axes
        proj_corrected_vecs = np.zeros_like(components)
        for i in range(n_components):
            vec = components[i].copy()
            # subtract the previous projections
            for j in range(i):
                vec -= np.dot(unit_vecs[j], vec)*unit_vecs[j]

            proj_corrected_vecs[i] = vec

        # get estimated variance of Y which is matrix product of feature vector
        # and the components (projection corrected)
        Y = np.tensordot(X, proj_corrected_vecs.T, axes=(1, 0))
        YYT = np.tensordot(Y.T, Y, axes=(1, 0))
        explained_variance = np.diag(YYT)/(n_samples-1)

        return explained_variance


class MiniBatchSparsePCA(SparsePCA):
    """Mini-batch Sparse Principal Components Analysis

    Finds the set of sparse components that can optimally reconstruct
    the data.  The amount of sparseness is controllable by the coefficient
    of the L1 penalty, given by the parameter alpha.

    Read more in the :ref:`User Guide <SparsePCA>`.

    Parameters
    ----------
    n_components : int,
        number of sparse atoms to extract

    alpha : int,
        Sparsity controlling parameter. Higher values lead to sparser
        components.

    ridge_alpha : float,
        Amount of ridge shrinkage to apply in order to improve
        conditioning when calling the transform method.

    n_iter : int,
        number of iterations to perform for each mini batch

    callback : callable or None, optional (default: None)
        callable that gets invoked every five iterations

    batch_size : int,
        the number of features to take in each mini batch

    verbose : int
        Controls the verbosity; the higher, the more messages. Defaults to 0.

    shuffle : boolean,
        whether to shuffle the data before splitting it in batches

    n_jobs : int,
        number of parallel jobs to run, or -1 to autodetect.

    method : {'lars', 'cd'}
        lars: uses the least angle regression method to solve the lasso problem
        (linear_model.lars_path)
        cd: uses the coordinate descent method to compute the
        Lasso solution (linear_model.Lasso). Lars will be faster if
        the estimated components are sparse.

    random_state : int, RandomState instance or None, optional (default=None)
        If int, random_state is the seed used by the random number generator;
        If RandomState instance, random_state is the random number generator;
        If None, the random number generator is the RandomState instance used
        by `np.random`.

    normalize_components : boolean, optional (default=False)
        - if False, use a version of Sparse PCA without components
          normalization and without data centering. This is likely a bug and
          even though it's the default for backward compatibility,
          this should not be used.
        - if True, use a version of Sparse PCA with components normalization
          and data centering.

        .. versionadded:: 0.20

        .. deprecated:: 0.22
           ``normalize_components`` was added and set to ``False`` for
           backward compatibility. It would be set to ``True`` from 0.22
           onwards.

    compute_variance : bool, optional (default=True)
        If true, this will compute the explained variance versus component.

    Attributes
    ----------
    components_ : array, shape (n_components, n_features)
        Sparse components extracted from the data.

    n_iter_ : int
        Number of iterations run.

    mean_ : array, shape (n_features, )
        Per-feature empirical mean, estimated from the training set.
        Equal to ``X.mean(axis=0)``.

    explained_variance_ : array, shape (n_components, )
        The explained variance versus component.
        Only computed if variance is set to True

    See also
    --------
    PCA
    SparsePCA
    DictionaryLearning
    """
    def __init__(self, n_components=None, alpha=1, ridge_alpha=0.01,
                 n_iter=100, callback=None, batch_size=3, verbose=False,
                 shuffle=True, n_jobs=1, method='lars', random_state=None,
                 normalize_components=False, compute_variance=True):
        super(MiniBatchSparsePCA, self).__init__(
            n_components=n_components, alpha=alpha, verbose=verbose,
            ridge_alpha=ridge_alpha, n_jobs=n_jobs, method=method,
            random_state=random_state,
            normalize_components=normalize_components,
            compute_variance=compute_variance)
        self.n_iter = n_iter
        self.callback = callback
        self.batch_size = batch_size
        self.shuffle = shuffle

    def fit(self, X, y=None):
        """Fit the model from data in X.

        Parameters
        ----------
        X : array-like, shape (n_samples, n_features)
            Training vector, where n_samples in the number of samples
            and n_features is the number of features.

        y : Ignored

        Returns
        -------
        self : object
            Returns the instance itself.
        """
        random_state = check_random_state(self.random_state)
        X = check_array(X)

        if self.normalize_components:
            self.mean_ = X.mean(axis=0)
            X = X - self.mean_
        else:
            warnings.warn("normalize_components=False is a "
                          "backward-compatible setting that implements a "
                          "non-standard definition of sparse PCA. This "
                          "compatibility mode will be removed in 0.22.",
                          DeprecationWarning)

        self.n_samples = X.shape[0]
        if self.n_components is None:
            n_components = X.shape[1]
        else:
            n_components = self.n_components
        Vt, _, self.n_iter_ = dict_learning_online(
            X.T, n_components, alpha=self.alpha,
            n_iter=self.n_iter, return_code=True,
            dict_init=None, verbose=self.verbose,
            callback=self.callback,
            batch_size=self.batch_size,
            shuffle=self.shuffle,
            n_jobs=self.n_jobs, method=self.method,
            random_state=random_state,
            return_n_iter=True)
        self.components_ = Vt.T

        if self.compute_variance:
            self.explained_variance_ = self._get_explained_variance(X)
        else:
            self.explained_variance_ = None

        if self.normalize_components:
            components_norm = \
                    np.linalg.norm(self.components_, axis=1)[:, np.newaxis]
            components_norm[components_norm == 0] = 1
            self.components_ /= components_norm

        return self
