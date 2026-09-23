// Profile editor: crops a new profile picture to a square before it uploads
(function () {
    'use strict';

    const fileInput = document.getElementById('profileImageFile');
    const cropArea = document.getElementById('cropArea');
    const cropImage = document.getElementById('cropImage');
    const avatar = document.querySelector('#avatarPreview .avatar');
    let cropper = null;

    fileInput.addEventListener('change', function () {
        const file = fileInput.files[0];
        if (!file) {
            return;
        }
        const reader = new FileReader();
        reader.onload = function (e) {
            cropImage.src = e.target.result;
            cropArea.hidden = false;
            if (cropper) {
                cropper.destroy();
            }
            cropper = new Cropper(cropImage, { aspectRatio: 1, viewMode: 1, autoCropArea: 0.85 });
        };
        reader.readAsDataURL(file);
    });

    document.getElementById('applyCrop').addEventListener('click', function () {
        if (!cropper) {
            return;
        }
        cropper.getCroppedCanvas({ width: 400, height: 400 }).toBlob(function (blob) {
            // Swap the picked file for the cropped one so that's what the form uploads
            const transfer = new DataTransfer();
            transfer.items.add(new File([blob], 'profile.jpg', { type: 'image/jpeg' }));
            fileInput.files = transfer.files;

            const img = document.createElement('img');
            img.src = URL.createObjectURL(blob);
            img.alt = 'New profile picture';
            avatar.replaceChildren(img);
            closeCropper();
        }, 'image/jpeg', 0.92);
    });

    document.getElementById('cancelCrop').addEventListener('click', function () {
        fileInput.value = '';
        closeCropper();
    });

    function closeCropper() {
        cropArea.hidden = true;
        if (cropper) {
            cropper.destroy();
            cropper = null;
        }
    }
})();
